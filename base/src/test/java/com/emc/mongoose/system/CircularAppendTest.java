package com.emc.mongoose.system;

import static com.emc.mongoose.util.LogValidationUtil.getMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsStdout;
import static com.emc.mongoose.util.LogValidationUtil.testMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceRecord;
import static com.emc.mongoose.util.LogValidationUtil.testTotalMetricsLogRecord;
import static com.emc.mongoose.util.TestCaseUtil.snakeCaseName;
import static com.emc.mongoose.util.TestCaseUtil.stepId;
import static com.emc.mongoose.util.docker.MongooseContainer.CONTAINER_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.BUNDLED_DEFAULTS;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.systemTestContainerScenarioPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
import com.github.akurilov.commons.system.SizeInBytes;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.Frequency;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public final class CircularAppendTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = systemTestContainerScenarioPath(getClass());
	private final int EXPECTED_APPEND_COUNT = 50;
	private final long EXPECTED_COUNT = 200;
	private final int TIMEOUT_IN_MILLIS = 1_000_000;
	private final String ITEM_LIST_FILE_0 = snakeCaseName(getClass()) + "_0.csv";
	private final String ITEM_LIST_FILE_1 = snakeCaseName(getClass()) + "_1.csv";
	private final String HOST_ITEM_LIST_FILE_1 = HOST_SHARE_PATH + "/" + ITEM_LIST_FILE_1;
	private final String CONTAINER_ITEM_LIST_FILE_0 = CONTAINER_SHARE_PATH + "/" + ITEM_LIST_FILE_0;
	private final String CONTAINER_ITEM_LIST_FILE_1 = CONTAINER_SHARE_PATH + "/" + ITEM_LIST_FILE_1;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> additionalNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;

	public CircularAppendTest(
					final StorageType storageType,
					final RunMode runMode,
					final Concurrency concurrency,
					final ItemSize itemSize)
					throws Exception {
		stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		try {
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch (final IOException ignored) {}
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
		env.add("BASE_ITEMS_COUNT=" + EXPECTED_COUNT);
		env.add("APPEND_COUNT=" + EXPECTED_APPEND_COUNT);
		env.add("ITEM_LIST_FILE_0=" + CONTAINER_ITEM_LIST_FILE_0);
		env.add("ITEM_LIST_FILE_1=" + CONTAINER_ITEM_LIST_FILE_1);
		env.add("ITEM_DATA_SIZE=" + itemSize.getValue());
		final List<String> args = new ArrayList<>();
		switch (storageType) {
		case ATMOS:
		case S3:
		case SWIFT:
			try {
				final HttpStorageMockContainer storageMock = new HttpStorageMockContainer(
								HttpStorageMockContainer.DEFAULT_PORT,
								false,
								null,
								null,
								Character.MAX_RADIX,
								HttpStorageMockContainer.DEFAULT_CAPACITY,
								HttpStorageMockContainer.DEFAULT_CONTAINER_CAPACITY,
								HttpStorageMockContainer.DEFAULT_CONTAINER_COUNT_LIMIT,
								HttpStorageMockContainer.DEFAULT_FAIL_CONNECT_EVERY,
								HttpStorageMockContainer.DEFAULT_FAIL_RESPONSES_EVERY,
								0);
				final String addr = "127.0.0.1:" + HttpStorageMockContainer.DEFAULT_PORT;
				storageMocks.put(addr, storageMock);
			} catch (final Throwable cause) {
				cause.printStackTrace();
			}
			args.add(
							"--storage-net-node-addrs="
											+ storageMocks.keySet().stream().collect(Collectors.joining(",")));
			break;
		case FS:
			try {
				DirWithManyFilesDeleter.deleteExternal(
								MongooseEntryNodeContainer.getHostItemOutputPath(stepId));
			} catch (final Throwable t) {
				Assert.fail(t.toString());
			}
			break;
		}
		switch (runMode) {
		case DISTRIBUTED:
			for (int i = 1; i < runMode.getNodeCount(); i++) {
				final int port = MongooseAdditionalNodeContainer.DEFAULT_PORT + i;
				final MongooseAdditionalNodeContainer nodeSvc = new MongooseAdditionalNodeContainer(port);
				final String addr = "127.0.0.1:" + port;
				additionalNodes.put(addr, nodeSvc);
			}
			args.add(
							"--load-step-node-addrs="
											+ additionalNodes.keySet().stream().collect(Collectors.joining(",")));
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
		additionalNodes.values().forEach(AsyncRunnableBase::start);
		testContainer.start();
		testContainer.await(TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
	}

	@After
	public final void tearDown() throws Exception {
		testContainer.close();
		additionalNodes
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
		try {
			DirWithManyFilesDeleter.deleteExternal(
							MongooseEntryNodeContainer.getHostItemOutputPath(stepId));
		} catch (final Throwable t) {
			Assert.fail(t.toString());
		}
	}

	@Test
	public final void test() throws Exception {
		try {
			final List<CSVRecord> metricsLogRecords = getMetricsLogRecords(stepId);
			assertTrue(
							"There should be more than 0 metrics records in the log file",
							metricsLogRecords.size() > 0);
			final int outputMetricsAveragePeriod;
			final Object outputMetricsAveragePeriodRaw = BUNDLED_DEFAULTS.val("output-metrics-average-period");
			final long expectedMaxCount = (long) (1.2 * (EXPECTED_APPEND_COUNT * EXPECTED_COUNT));
			if (outputMetricsAveragePeriodRaw instanceof String) {
				outputMetricsAveragePeriod = (int) TimeUtil.getTimeInSeconds((String) outputMetricsAveragePeriodRaw);
			} else {
				outputMetricsAveragePeriod = TypeUtil.typeConvert(outputMetricsAveragePeriodRaw, int.class);
			}
			testMetricsLogRecords(
							metricsLogRecords,
							OpType.UPDATE,
							concurrency.getValue(),
							runMode.getNodeCount(),
							itemSize.getValue(),
							expectedMaxCount,
							0,
							outputMetricsAveragePeriod);
		} catch (final FileNotFoundException ignored) {
			// there may be no metrics file if append step duration is less than 10s
		}
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords(stepId);
		assertEquals(
						"There should be 1 total metrics records in the log file",
						1,
						totalMetrcisLogRecords.size());
		testTotalMetricsLogRecord(
						totalMetrcisLogRecords.get(0),
						OpType.UPDATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						0,
						0,
						0);
		final String stdOutContent = testContainer.stdOutContent();
		testFinalMetricsStdout(
						stdOutContent,
						OpType.UPDATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						stepId);
		final LongAdder opTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> opTraceRecTestFunc = ioTraceRec -> {
			testOpTraceRecord(ioTraceRec, OpType.UPDATE.ordinal(), itemSize.getValue());
			opTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepId, opTraceRecTestFunc);
		assertTrue(
						"There should be more than "
										+ EXPECTED_COUNT
										+ " records in the I/O trace log file, but got: "
										+ opTraceRecCount.sum(),
						EXPECTED_COUNT < opTraceRecCount.sum());
		final List<CSVRecord> items = new ArrayList<>();
		try (final BufferedReader br = new BufferedReader(new FileReader(HOST_ITEM_LIST_FILE_1))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for (final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");
		final Frequency freq = new Frequency();
		String itemPath, itemId;
		long itemOffset;
		long size;
		final SizeInBytes expectedFinalSize = new SizeInBytes(
						(EXPECTED_APPEND_COUNT + 1) * itemSize.getValue().get() / 3,
						(EXPECTED_APPEND_COUNT + 1) * itemSize.getValue().get() * 3,
						1);
		final int n = items.size();
		CSVRecord itemRec;
		for (int i = 0; i < n; i++) {
			itemRec = items.get(i);
			itemPath = itemRec.get(0);
			for (int j = i; j < n; j++) {
				if (i != j) {
					assertNotEquals(itemPath, items.get(j).get(0));
				}
			}
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			if (!storageType.equals(StorageType.ATMOS)) {
				itemOffset = Long.parseLong(itemRec.get(1), 0x10);
				assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
				freq.addValue(itemOffset);
			}
			size = Long.parseLong(itemRec.get(2));
			assertTrue(
							"Expected size: " + expectedFinalSize.toString() + ", actual: " + size,
							expectedFinalSize.getMin() <= size && size <= expectedFinalSize.getMax());
			assertEquals("0/0", itemRec.get(3));
		}
		if (!storageType.equals(StorageType.ATMOS)) {
			assertEquals(EXPECTED_COUNT, freq.getUniqueCount(), EXPECTED_COUNT / 10);
		}
	}
}
