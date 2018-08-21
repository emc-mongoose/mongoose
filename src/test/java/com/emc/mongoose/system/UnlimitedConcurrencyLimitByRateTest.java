package com.emc.mongoose.system;

import com.emc.mongoose.config.BundledDefaultsProvider;
import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.EnvParams;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;
import com.emc.mongoose.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseAdditionalNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.item.op.OpType.CREATE;
import static com.emc.mongoose.system.util.LogValidationUtil.getMetricsLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.testMetricsLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.testMetricsTableStdout;
import static com.emc.mongoose.system.util.LogValidationUtil.testFinalMetricsStdout;
import static com.emc.mongoose.system.util.LogValidationUtil.testTotalMetricsLogRecord;
import static com.emc.mongoose.system.util.TestCaseUtil.stepId;
import static com.emc.mongoose.system.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.system.util.docker.MongooseContainer.containerScenarioPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class) public class UnlimitedConcurrencyLimitByRateTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = containerScenarioPath(getClass());
	private static final int COUNT_LIMIT = 1_000_000;
	private static final SizeInBytes SIZE_LIMIT = new SizeInBytes("10GB");
	private static final int TIME_LIMIT_SEC = 60;
	private static final int RATE_LIMIT = 1_000;
	private final int timeoutInMillis = 1000_000;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;
	private final int averagePeriod;
	private final Config config;
	private final String hostItemOutputPath = MongooseContainer.getHostItemOutputPath(getClass().getSimpleName());
	private final String hostItemOutputFile = HOST_SHARE_PATH + "/" + getClass().getSimpleName() + ".csv";
	private long duration;
	private String stdOutContent = null;

	public UnlimitedConcurrencyLimitByRateTest(
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
		try {
			FileUtils.deleteDirectory(Paths.get(MongooseContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch(final IOException ignored) {
		}
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
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
			case FS:
				args.add("--item-output-path=" + hostItemOutputPath);
				try {
					DirWithManyFilesDeleter.deleteExternal(hostItemOutputPath);
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
				break;
		}
		switch(runMode) {
			case DISTRIBUTED:
				for(int i = 1; i < runMode.getNodeCount(); i++) {
					final int port = MongooseAdditionalNodeContainer.DEFAULT_PORT + i;
					final MongooseAdditionalNodeContainer nodeSvc = new MongooseAdditionalNodeContainer(port);
					final String addr = "127.0.0.1:" + port;
					slaveNodes.put(addr, nodeSvc);
				}
				args.add("--load-step-node-addrs=" + slaveNodes.keySet().stream().collect(Collectors.joining(",")));
				break;
		}
		testContainer = new MongooseContainer(
			stepId, storageType, runMode, concurrency, itemSize, SCENARIO_PATH, env, args
		);
	}

	@Before
	public final void setUp()
	throws Exception {
		storageMocks.values().forEach(AsyncRunnableBase::start);
		slaveNodes.values().forEach(AsyncRunnableBase::start);
		duration = System.currentTimeMillis();
		testContainer.start();
		testContainer.await(timeoutInMillis, TimeUnit.MILLISECONDS);
		duration = System.currentTimeMillis() - duration;
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
		testMetricsTableStdout(
			stdOutContent, stepId, storageType, runMode.getNodeCount(), COUNT_LIMIT,
			new HashMap<OpType, Integer>() {{
				put(CREATE, 0);
			}}
		);
		testFinalMetricsStdout(stdOutContent, CREATE, 0, runMode.getNodeCount(), itemSize.getValue(), stepId);
		final List<CSVRecord> metricsLogRecs = getMetricsLogRecords(stepId);
		testMetricsLogRecords(
			metricsLogRecs, CREATE, 0, runMode.getNodeCount(), itemSize.getValue(), COUNT_LIMIT, TIME_LIMIT_SEC,
			averagePeriod
		);
		final List<CSVRecord> totalMetricsRecs = getMetricsTotalLogRecords(stepId);
		assertEquals(totalMetricsRecs.size(), 1);
		final CSVRecord totalMetricsRec = totalMetricsRecs.get(0);
		testTotalMetricsLogRecord(
			totalMetricsRec, CREATE, 0, runMode.getNodeCount(), itemSize.getValue(), COUNT_LIMIT, TIME_LIMIT_SEC
		);
		final double rate = Double.parseDouble(totalMetricsRec.get("TPAvg[op/s]"));
		assertTrue(rate < RATE_LIMIT + RATE_LIMIT / 2);
		final long totalSize = Long.parseLong(totalMetricsRec.get("Size"));
		if(StorageType.FS.equals(storageType)) {
			assertTrue(totalSize < SIZE_LIMIT.get() + SIZE_LIMIT.get() / 10);
		}
		final long durationInSec = TimeUnit.MILLISECONDS.toSeconds(duration);
		assertTrue(
			"Test time was " + durationInSec + " while expected no more than " + TIME_LIMIT_SEC,
			TIME_LIMIT_SEC + 5 >= durationInSec
		);
	}
}
