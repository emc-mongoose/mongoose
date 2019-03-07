package com.emc.mongoose.system;

import static com.emc.mongoose.util.LogValidationUtil.getMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsStdout;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsTableRowStdout;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public final class ReadUsingVariablePathTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private static final String SCENARIO_PATH = systemTestContainerScenarioPath(ReadUsingVariablePathTest.class);
	private static final String ITEM_LIST_FILE = CONTAINER_SHARE_PATH + "/" + snakeCaseName(ReadUsingVariablePathTest.class) + ".csv";
	private static final int EXPECTED_COUNT = 10_000;
	private static final int TIMEOUT_MILLIS = 1_000_000;
	private static final String CONTAINER_ITEM_OUTPUT_PATH = MongooseEntryNodeContainer.getContainerItemOutputPath(
					ReadUsingVariablePathTest.class.getSimpleName());
	private static final String HOST_ITEM_OUTPUT_PATH = MongooseEntryNodeContainer.getHostItemOutputPath(
					ReadUsingVariablePathTest.class.getSimpleName());

	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;

	public ReadUsingVariablePathTest(
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
		new File(ITEM_LIST_FILE.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString())).delete();
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
		env.add("ITEM_DATA_SIZE=" + itemSize.getValue());
		env.add("ITEM_LIST_FILE=" + ITEM_LIST_FILE);
		env.add("ITEM_OUTPUT_PATH=" + CONTAINER_ITEM_OUTPUT_PATH);
		env.add("STEP_LIMIT_COUNT=" + EXPECTED_COUNT);
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
							Character.MAX_RADIX,
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
			args.add("--item-output-path=" + CONTAINER_ITEM_OUTPUT_PATH);
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
		testContainer.start();
		testContainer.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	@After
	public final void tearDown() throws Exception {
		testContainer.close();
		slaveNodes
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
		final LongAdder opTraceRecCount = new LongAdder();
		final int baseOutputPathLen = CONTAINER_ITEM_OUTPUT_PATH.length();
		// Item path should look like:
		// ${FILE_OUTPUT_PATH}/1/b/0123456789abcdef
		// ${FILE_OUTPUT_PATH}/b/fedcba9876543210
		final Pattern subPathPtrn = Pattern.compile("(/[0-9a-f]){1,2}/[0-9a-f]{16}");
		final Consumer<CSVRecord> opTraceReqTestFunc = opTraceRec -> {
			testOpTraceRecord(opTraceRec, OpType.READ.ordinal(), itemSize.getValue());
			String nextFilePath = opTraceRec.get(1);
			assertTrue(nextFilePath.startsWith(CONTAINER_ITEM_OUTPUT_PATH));
			nextFilePath = nextFilePath.substring(baseOutputPathLen);
			final Matcher m = subPathPtrn.matcher(nextFilePath);
			assertTrue(m.matches());
			opTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepId, opTraceReqTestFunc);
		assertEquals(
						"There should be more than 1 record in the I/O trace log file",
						EXPECTED_COUNT,
						opTraceRecCount.sum());
		final int outputMetricsAveragePeriod;
		final Object outputMetricsAveragePeriodRaw = BUNDLED_DEFAULTS.val("output-metrics-average-period");
		if (outputMetricsAveragePeriodRaw instanceof String) {
			outputMetricsAveragePeriod = (int) TimeUtil.getTimeInSeconds((String) outputMetricsAveragePeriodRaw);
		} else {
			outputMetricsAveragePeriod = TypeUtil.typeConvert(outputMetricsAveragePeriodRaw, int.class);
		}
		testMetricsLogRecords(
						getMetricsLogRecords(stepId),
						OpType.READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						EXPECTED_COUNT,
						0,
						outputMetricsAveragePeriod);
		testTotalMetricsLogRecord(
						getMetricsTotalLogRecords(stepId).get(0),
						OpType.READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						EXPECTED_COUNT,
						0,
						0);
		final String stdOutContent = testContainer.stdOutContent();
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
						OpType.CREATE,
						runMode.getNodeCount(),
						concurrency.getValue(),
						EXPECTED_COUNT,
						0,
						itemSize.getValue());
	}
}
