package com.emc.mongoose.system;

import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.EnvParams;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseSlaveNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.Frequency;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.emc.mongoose.system.util.LogValidationUtil.getMetricsLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.testIoTraceLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.testIoTraceRecord;
import static com.emc.mongoose.system.util.LogValidationUtil.testMetricsLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.testSingleMetricsStdout;
import static com.emc.mongoose.system.util.LogValidationUtil.testTotalMetricsLogRecord;
import static com.emc.mongoose.system.util.TestCaseUtil.snakeCaseName;
import static com.emc.mongoose.system.util.TestCaseUtil.stepId;
import static com.emc.mongoose.system.util.docker.MongooseContainer.BUNDLED_DEFAULTS;
import static com.emc.mongoose.system.util.docker.MongooseContainer.CONTAINER_SHARE_PATH;
import static com.emc.mongoose.system.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.system.util.docker.MongooseContainer.containerScenarioPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class) public final class CircularAppendTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = containerScenarioPath(getClass());
	private final int EXPECTED_APPEND_COUNT = 100;
	private final long EXPECTED_COUNT = 100;
	private final int timeoutInMillis = 1000_000;
	private final String itemListFile0 = snakeCaseName(getClass()) + "_0.csv";
	private final String itemListFile1 = snakeCaseName(getClass()) + "_1.csv";
	private final String hostItemListFile0 = HOST_SHARE_PATH + "/" + itemListFile0;
	private final String hostItemListFile1 = HOST_SHARE_PATH + "/" + itemListFile1;
	private final String containerItemListFile0 = CONTAINER_SHARE_PATH + "/" + itemListFile0;
	private final String containerItemListFile1 = CONTAINER_SHARE_PATH + "/" + itemListFile1;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseSlaveNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;

	public CircularAppendTest(
		final StorageType storageType, final RunMode runMode, final Concurrency concurrency, final ItemSize itemSize
							 )
	throws Exception {
		stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		try {
			FileUtils.deleteDirectory(Paths.get(MongooseContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch(final IOException ignored) {
		}
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
		final List<String> env =
			System.getenv().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList());
		env.add("BASE_ITEMS_COUNT=" + EXPECTED_COUNT);
		env.add("APPEND_COUNT=" + EXPECTED_APPEND_COUNT);
		env.add("ITEM_LIST_FILE_0=" + containerItemListFile0);
		env.add("ITEM_LIST_FILE_1=" + containerItemListFile1);
		env.add("ITEM_DATA_SIZE=" + itemSize.getValue());
		final List<String> args = new ArrayList<>();
		switch(storageType) {
			case ATMOS:
			case S3:
			case SWIFT:
				final HttpStorageMockContainer storageMock =
					new HttpStorageMockContainer(HttpStorageMockContainer.DEFAULT_PORT, false, null, null,
												 Character.MAX_RADIX, HttpStorageMockContainer.DEFAULT_CAPACITY,
												 HttpStorageMockContainer.DEFAULT_CONTAINER_CAPACITY,
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
		try {
			final List<CSVRecord> metricsLogRecords = getMetricsLogRecords(stepId);
			assertTrue(
				"There should be more than 0 metrics records in the log file",
				metricsLogRecords.size() > 0
					  );
			final int outputMetricsAveragePeriod;
			final Object outputMetricsAveragePeriodRaw = BUNDLED_DEFAULTS.val("output-metrics-average-period");
			final long expectedMaxCount = (long) (1.1 * (EXPECTED_APPEND_COUNT * EXPECTED_COUNT));
			if(outputMetricsAveragePeriodRaw instanceof String) {
				outputMetricsAveragePeriod = (int) TimeUtil.getTimeInSeconds((String) outputMetricsAveragePeriodRaw);
			} else {
				outputMetricsAveragePeriod = TypeUtil.typeConvert(outputMetricsAveragePeriodRaw, int.class);
			}
			testMetricsLogRecords(
				metricsLogRecords, OpType.UPDATE, concurrency.getValue(), runMode.getNodeCount(),
				itemSize.getValue(), expectedMaxCount, 0,
				outputMetricsAveragePeriod
								 );
		} catch(final FileNotFoundException ignored) {
			//there may be no metrics file if append step duration is less than 10s
		}
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords(stepId);
		assertEquals("There should be 1 total metrics records in the log file", 1, totalMetrcisLogRecords.size());
		testTotalMetricsLogRecord(totalMetrcisLogRecords.get(0), OpType.UPDATE, concurrency.getValue(),
								  runMode.getNodeCount(), itemSize.getValue(), 0, 0
								 );
		final String stdOutContent = testContainer.stdOutContent();
		final int outputMetricsAveragePeriod;
		final Object outputMetricsAveragePeriodRaw = BUNDLED_DEFAULTS.val("output-metrics-average-period");
		if(outputMetricsAveragePeriodRaw instanceof String) {
			outputMetricsAveragePeriod = (int) TimeUtil.getTimeInSeconds((String) outputMetricsAveragePeriodRaw);
		} else {
			outputMetricsAveragePeriod = TypeUtil.typeConvert(outputMetricsAveragePeriodRaw, int.class);
		}
		testSingleMetricsStdout(
			stdOutContent.replaceAll("[\r\n]+", " "), OpType.UPDATE, concurrency.getValue(),
			runMode.getNodeCount(), itemSize.getValue(), outputMetricsAveragePeriod
							   );
		final LongAdder ioTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, OpType.UPDATE.ordinal(), itemSize.getValue());
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(stepId, ioTraceReqTestFunc);
		assertTrue(
			"There should be more than " + EXPECTED_COUNT + " records in the I/O trace log file, but got: " +
				ioTraceRecCount.sum(),
			EXPECTED_COUNT < ioTraceRecCount.sum()
				  );
		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(hostItemListFile1))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");
		final Frequency freq = new Frequency();
		String itemPath, itemId;
		long itemOffset;
		long size;
		final SizeInBytes expectedFinalSize =
			new SizeInBytes(
				(EXPECTED_APPEND_COUNT + 1) * itemSize.getValue().get() / 2,
				4 * (EXPECTED_APPEND_COUNT + 1) * itemSize.getValue().get(),
				1
			);
		final int n = items.size();
		CSVRecord itemRec;
		for(int i = 0; i < n; i++) {
			itemRec = items.get(i);
			itemPath = itemRec.get(0);
			for(int j = i; j < n; j++) {
				if(i != j) {
					assertFalse(itemPath.equals(items.get(j).get(0)));
				}
			}
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			if(! storageType.equals(StorageType.ATMOS)) {
				itemOffset = Long.parseLong(itemRec.get(1), 0x10);
				assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
				freq.addValue(itemOffset);
			}
			size = Long.parseLong(itemRec.get(2));
			assertTrue(
				"Expected size: " + expectedFinalSize.toString() + ", actual: " + size,
				expectedFinalSize.getMin() <= size && size <= expectedFinalSize.getMax()
					  );
			assertEquals("0/0", itemRec.get(3));
		}
		if(! storageType.equals(StorageType.ATMOS)) {
			assertEquals(EXPECTED_COUNT, freq.getUniqueCount(), EXPECTED_COUNT / 20);
		}
	}
}
