package com.emc.mongoose.system;

import com.emc.mongoose.config.BundledDefaultsProvider;
import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.*;
import com.emc.mongoose.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseSlaveNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.system.util.LogValidationUtil.testSingleMetricsStdout;
import static com.emc.mongoose.system.util.TestCaseUtil.stepId;
import static com.emc.mongoose.system.util.docker.MongooseContainer.*;

@RunWith(Parameterized.class) public class MultipleFixedUpdateAndSingleFixedReadTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = containerScenarioPath(getClass());
	private final int timeoutInMillis = 1000_000;
	private final long EXPECTED_COUNT = 2000;
	private final SizeInBytes expectedUpdateSize;
	private final SizeInBytes expectedReadSize;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseSlaveNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;
	private final Config config;
	private final String containerItemOutputPath;
	private final String hostItemOutputFile =
		HOST_SHARE_PATH + "/" + CreateLimitBySizeTest.class.getSimpleName() + ".csv";
	private final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");
	private int averagePeriod;
	private String stdOutContent = null;

	public MultipleFixedUpdateAndSingleFixedReadTest(
		final StorageType storageType, final RunMode runMode, final Concurrency concurrency, final ItemSize itemSize
	)
	throws Exception {
		final Map<String, Object> schema =
			SchemaProvider.resolveAndReduce(APP_NAME, Thread.currentThread().getContextClassLoader());
		config = new BundledDefaultsProvider().config(ARG_PATH_SEP, schema);
		final Object avgPeriodRaw = config.val("output-metrics-average-period");
		if(avgPeriodRaw instanceof String) {
			averagePeriod = (int) TimeUtil.getTimeInSeconds((String) avgPeriodRaw);
		} else {
			averagePeriod = TypeUtil.typeConvert(avgPeriodRaw, int.class);
		}
		stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		containerItemOutputPath = MongooseContainer.getContainerItemOutputPath(stepId);
		try {
			FileUtils.deleteDirectory(Paths.get(MongooseContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch(final IOException ignored) {
		}
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
		this.expectedUpdateSize =
			new SizeInBytes(- LongStream.of(2 - 5, 10 - 20, 50 - 100, 200 - 500, 1000 - 2000).sum());
		this.expectedReadSize = new SizeInBytes(itemSize.getValue().get() - 256);
		if(storageType.equals(StorageType.FS)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(containerItemOutputPath);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		try {
			Files.delete(Paths.get(hostItemOutputFile));
		} catch(final Exception ignored) {
		}
		final List<String> env =
			System.getenv().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList());
		final List<String> args = new ArrayList<>();
		switch(storageType) {
			case ATMOS:
			case S3:
			case SWIFT:
				final HttpStorageMockContainer storageMock =
					new HttpStorageMockContainer(HttpStorageMockContainer.DEFAULT_PORT, false, null, null, itemIdRadix,
						HttpStorageMockContainer.DEFAULT_CAPACITY, HttpStorageMockContainer.DEFAULT_CONTAINER_CAPACITY,
						HttpStorageMockContainer.DEFAULT_CONTAINER_COUNT_LIMIT,
						HttpStorageMockContainer.DEFAULT_FAIL_CONNECT_EVERY,
						HttpStorageMockContainer.DEFAULT_FAIL_RESPONSES_EVERY, 0
					);
				final String addr = "127.0.0.1:" + HttpStorageMockContainer.DEFAULT_PORT;
				storageMocks.put(addr, storageMock);
				args.add("--storage-net-node-addrs=" + storageMocks.keySet().stream().collect(Collectors.joining(",")));
				break;
		}
		switch(runMode) {
			case DISTRIBUTED:
				final String localExternalAddr = ServiceUtil.getAnyExternalHostAddress();
				for(int i = 1; i < runMode.getNodeCount(); i++) {
					final int port = MongooseSlaveNodeContainer.DEFAULT_PORT + i;
					final MongooseSlaveNodeContainer nodeSvc = new MongooseSlaveNodeContainer(port);
					final String addr = localExternalAddr + ":" + port;
					slaveNodes.put(addr, nodeSvc);
				}
				args.add("--load-step-node-addrs=" + slaveNodes.keySet().stream().collect(Collectors.joining(",")));
				break;
		}
		testContainer =
			new MongooseContainer(stepId, storageType, runMode, concurrency, itemSize, SCENARIO_PATH, env, args);
	}

	@Before
	public final void setUp()
	throws Exception {
		storageMocks.values().forEach(AsyncRunnableBase::start);
		slaveNodes.values().forEach(AsyncRunnableBase::start);
		testContainer.start();
		testContainer.await(timeoutInMillis, TimeUnit.MILLISECONDS);
		stdOutContent = testContainer.stdOutContent();
	}

	@After
	public final void tearDown()
	throws Exception {
		testContainer.close();
		slaveNodes.values().parallelStream().forEach(storageMock -> {
			try {
				storageMock.close();
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			}
		});
		storageMocks.values().parallelStream().forEach(storageMock -> {
			try {
				storageMock.close();
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			}
		});
	}

	@Test
	public final void test()
	throws Exception {
		// I/O traces
		//        final LongAdder ioTraceRecCount = new LongAdder();
		//        final Consumer<CSVRecord> ioTraceRecTestFunc = ioTraceRec -> {
		//            if (ioTraceRecCount.sum() < EXPECTED_COUNT) {
		//                testIoTraceRecord(ioTraceRec, OpType.UPDATE.ordinal(), expectedUpdateSize);
		//            } else {
		//                testIoTraceRecord(ioTraceRec, OpType.READ.ordinal(), expectedReadSize);
		//            }
		//            ioTraceRecCount.increment();
		//        };
		//        testIoTraceLogRecords(stepId, ioTraceRecTestFunc);
		//        assertEquals(
		//                "There should be " + 2 * EXPECTED_COUNT + " records in the I/O trace log file",
		//                2 * EXPECTED_COUNT, ioTraceRecCount.sum()
		//        );
		//
		//        final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords(stepId);
		//        testTotalMetricsLogRecord(
		//                totalMetrcisLogRecords.get(0), OpType.UPDATE, concurrency.getValue(),
		//                runMode.getNodeCount(), expectedUpdateSize, EXPECTED_COUNT, 0
		//        );
		//        testTotalMetricsLogRecord(
		//                totalMetrcisLogRecords.get(1), OpType.READ, concurrency.getValue(),
		//                runMode.getNodeCount(), expectedReadSize, EXPECTED_COUNT, 0
		//        );
		//
		//        final List<CSVRecord> metricsLogRecords = getMetricsLogRecords(stepId);
		//        final List<CSVRecord> updateMetricsRecords = new ArrayList<>();
		//        final List<CSVRecord> readMetricsRecords = new ArrayList<>();
		//        for (final CSVRecord metricsLogRec : metricsLogRecords) {
		//            if (OpType.UPDATE.name().equalsIgnoreCase(metricsLogRec.get("OpType"))) {
		//                updateMetricsRecords.add(metricsLogRec);
		//            } else {
		//                readMetricsRecords.add(metricsLogRec);
		//            }
		//        }
		//        testMetricsLogRecords(
		//                updateMetricsRecords, OpType.UPDATE, concurrency.getValue(), runMode.getNodeCount(),
		//                expectedUpdateSize, EXPECTED_COUNT, 0,
		//                averagePeriod
		//        );
		//        testMetricsLogRecords(
		//                readMetricsRecords, OpType.READ, concurrency.getValue(), runMode.getNodeCount(),
		//                expectedReadSize, EXPECTED_COUNT, 0,
		//                averagePeriod
		//        );
		//
		final String stdOutput = stdOutContent.replaceAll("[\r\n]+", " ");
		testSingleMetricsStdout(stdOutput, OpType.UPDATE, concurrency.getValue(), runMode.getNodeCount(),
			expectedUpdateSize, averagePeriod
		);
		testSingleMetricsStdout(stdOutput, OpType.READ, concurrency.getValue(), runMode.getNodeCount(),
			expectedReadSize, averagePeriod
		);
	}
}
