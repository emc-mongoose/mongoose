package com.emc.mongoose.system;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static com.emc.mongoose.base.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsStdout;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsTableRowStdout;
import static com.emc.mongoose.util.LogValidationUtil.testMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceRecord;
import static com.emc.mongoose.util.LogValidationUtil.testTotalMetricsLogRecord;
import static com.emc.mongoose.util.TestCaseUtil.stepId;
import static com.emc.mongoose.util.docker.MongooseContainer.CONTAINER_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.BUNDLED_DEFAULTS;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.systemTestContainerScenarioPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.emc.mongoose.base.config.BundledDefaultsProvider;
import com.emc.mongoose.base.config.TimeUtil;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.params.Concurrency;
import com.emc.mongoose.params.EnvParams;
import com.emc.mongoose.params.ItemSize;
import com.emc.mongoose.params.RunMode;
import com.emc.mongoose.params.StorageType;
import com.emc.mongoose.util.DirWithManyFilesDeleter;
import com.emc.mongoose.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.util.docker.MongooseAdditionalNodeContainer;
import com.emc.mongoose.util.docker.MongooseEntryNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CircularReadLimitByTimeTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = systemTestContainerScenarioPath(getClass());
	private final int TIMEOUT_IN_MILLIS = 1000_000;
	private final int timeLimitInSec = 65; // 1m + up to 5s for the precondition job
	private final String ITEM_OUTPUT_FILE = "/CircularReadLimitByTime.csv";
	private final String HOST_ITEM_OUTPUT_PATH;
	private final String HOST_ITEM_OUTPUT_FILE = HOST_SHARE_PATH + "/" + getClass().getSimpleName() + ".csv";
	private final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;
	private final Config config;
	private final int averagePeriod;
	private boolean finishedInTime;
	private String stdOutContent = null;

	public CircularReadLimitByTimeTest(
					final StorageType storageType,
					final RunMode runMode,
					final Concurrency concurrency,
					final ItemSize itemSize)
					throws Exception {
		final Map<String, Object> schema = SchemaProvider.resolveAndReduce(APP_NAME, Thread.currentThread().getContextClassLoader());
		config = new BundledDefaultsProvider().config(ARG_PATH_SEP, schema);
		final Object avgPeriodRaw = config.val("output-metrics-average-period");
		if (avgPeriodRaw instanceof String) {
			averagePeriod = (int) TimeUtil.getTimeInSeconds((String) avgPeriodRaw);
		} else {
			averagePeriod = TypeUtil.typeConvert(avgPeriodRaw, int.class);
		}
		stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		HOST_ITEM_OUTPUT_PATH = MongooseEntryNodeContainer.getHostItemOutputPath(getClass().getSimpleName());
		try {
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch (final IOException ignored) {}
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
		if (storageType.equals(StorageType.FS)) {}
		try {
			Files.delete(Paths.get(HOST_ITEM_OUTPUT_FILE));
		} catch (final Exception ignored) {}
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
		env.add("FILE_NAME=" + CONTAINER_SHARE_PATH + ITEM_OUTPUT_FILE);
		final List<String> args = new ArrayList<>();
		switch (storageType) {
		case ATMOS:
		case S3:
		case SWIFT:
			final HttpStorageMockContainer storageMock = new HttpStorageMockContainer(
							HttpStorageMockContainer.DEFAULT_PORT,
							false,
							null,
							null,
							itemIdRadix,
							HttpStorageMockContainer.DEFAULT_CAPACITY,
							HttpStorageMockContainer.DEFAULT_CONTAINER_CAPACITY,
							HttpStorageMockContainer.DEFAULT_CONTAINER_COUNT_LIMIT,
							HttpStorageMockContainer.DEFAULT_FAIL_CONNECT_EVERY,
							HttpStorageMockContainer.DEFAULT_FAIL_RESPONSES_EVERY,
							0);
			final String addr = "127.0.0.1:" + HttpStorageMockContainer.DEFAULT_PORT;
			storageMocks.put(addr, storageMock);
			args.add(
							"--storage-net-node-addrs="
											+ storageMocks.keySet().stream().collect(Collectors.joining(",")));
			break;
		case FS:
			try {
				DirWithManyFilesDeleter.deleteExternal(HOST_ITEM_OUTPUT_PATH);
			} catch (final Exception e) {
				e.printStackTrace(System.err);
			}
			break;
		}
		switch (runMode) {
		case DISTRIBUTED:
			for (int i = 1; i < runMode.getNodeCount(); i++) {
				final int port = MongooseAdditionalNodeContainer.DEFAULT_PORT + i;
				final MongooseAdditionalNodeContainer nodeSvc = new MongooseAdditionalNodeContainer(port);
				final String addr = "127.0.0.1:" + port;
				slaveNodes.put(addr, nodeSvc);
			}
			args.add(
							"--load-step-node-addrs="
											+ slaveNodes.keySet().stream().collect(Collectors.joining(",")));
			break;
		}
		testContainer = new MongooseEntryNodeContainer(
						stepId,
						storageType,
						runMode,
						concurrency,
						itemSize.getValue(),
						SCENARIO_PATH,
						env,
						args);
	}

	@Before
	public final void setUp() throws Exception {
		storageMocks.values().forEach(AsyncRunnableBase::start);
		slaveNodes.values().forEach(AsyncRunnableBase::start);
		long duration = System.currentTimeMillis();
		testContainer.start();
		testContainer.await(TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
		duration = System.currentTimeMillis() - duration;
		finishedInTime = (TimeUnit.MILLISECONDS.toSeconds(duration) <= timeLimitInSec + 10);
		stdOutContent = testContainer.stdOutContent();
	}

	@After
	public final void tearDown() throws Exception {
		testContainer.close();
		slaveNodes
						.values()
						.parallelStream()
						.forEach(
										node -> {
											try {
												node.close();
											} catch (final Throwable t) {
												t.printStackTrace(System.err);
											}
										});
		storageMocks
						.values()
						.parallelStream()
						.forEach(
										storageMock -> {
											try {
												storageMock.close();
											} catch (final Throwable t) {
												t.printStackTrace(System.err);
											}
										});
	}

	@Test
	public final void test() throws Exception {
		final LongAdder ioTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			testOpTraceRecord(ioTraceRec, OpType.READ.ordinal(), itemSize.getValue());
			ioTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepId, ioTraceReqTestFunc);
		assertTrue(
						"There should be more than 1 record in the I/O trace log file", ioTraceRecCount.sum() > 1);
		final List<CSVRecord> items = new ArrayList<>();
		try (final BufferedReader br = new BufferedReader(new FileReader(HOST_SHARE_PATH + ITEM_OUTPUT_FILE))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for (final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		assertEquals(1, items.size());
		final CSVRecord itemRec = items.get(0);
		final String itemPath = itemRec.get(0);
		final String itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
		final long itemOffset = Long.parseLong(itemRec.get(1), 0x10);
		assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
		final long size = Long.parseLong(itemRec.get(2));
		assertEquals(itemSize.getValue().get(), size);
		final String modLayerAndMask = itemRec.get(3);
		assertEquals("0/0", modLayerAndMask);
		testFinalMetricsStdout(
						stdOutContent,
						OpType.READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						stepId);
		testFinalMetricsTableRowStdout(
						stdOutContent,
						stepId,
						OpType.READ,
						runMode.getNodeCount(),
						concurrency.getValue(),
						0,
						60,
						itemSize.getValue());
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords(stepId);
		assertEquals(
						"There should be 1 total metrics records in the log file",
						1,
						totalMetrcisLogRecords.size());
		testTotalMetricsLogRecord(
						totalMetrcisLogRecords.get(0),
						OpType.READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						0,
						1,
						60);
		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords(stepId);
		assertTrue(
						"There should be more than 2 metrics records in the log file",
						metricsLogRecords.size() > 1);
		testMetricsLogRecords(
						metricsLogRecords,
						OpType.READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						0,
						60,
						averagePeriod);
		assertTrue("Scenario didn't finished in time", finishedInTime);
	}
}
