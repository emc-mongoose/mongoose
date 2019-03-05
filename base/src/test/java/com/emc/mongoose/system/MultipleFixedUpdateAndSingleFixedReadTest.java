package com.emc.mongoose.system;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static com.emc.mongoose.base.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsStdout;
import static com.emc.mongoose.util.LogValidationUtil.testMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceRecord;
import static com.emc.mongoose.util.LogValidationUtil.testTotalMetricsLogRecord;
import static com.emc.mongoose.util.TestCaseUtil.stepId;
import static com.emc.mongoose.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.BUNDLED_DEFAULTS;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.systemTestContainerScenarioPath;
import static org.junit.Assert.assertEquals;

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
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
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
import java.util.stream.LongStream;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MultipleFixedUpdateAndSingleFixedReadTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = systemTestContainerScenarioPath(getClass());
	private final int TIMEOUT_IN_MILLIS = 1_000_000;
	private final long EXPECTED_COUNT = 2_000;
	private final String containerItemOutputPath = MongooseEntryNodeContainer.getContainerItemOutputPath(getClass().getSimpleName());
	private final String hostItemOutputPath = MongooseEntryNodeContainer.getHostItemOutputPath(getClass().getSimpleName());
	private final String HOST_ITEM_OUTPUT_FILE = HOST_SHARE_PATH + "/" + getClass().getSimpleName() + ".csv";
	private final SizeInBytes expectedUpdateSize;
	private final SizeInBytes expectedReadSize;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final String stepIdUpdate;
	private final String stepIdRead;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final Config config;
	private final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");
	private int averagePeriod;
	private String stdOutContent = null;

	public MultipleFixedUpdateAndSingleFixedReadTest(
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
		stepIdUpdate = stepId + "_UPDATE";
		stepIdRead = stepId + "_READ";
		try {
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepId).toFile());
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepIdUpdate).toFile());
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepIdRead).toFile());
		} catch (final IOException ignored) {}
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.expectedUpdateSize = new SizeInBytes(-LongStream.of(2 - 5, 10 - 20, 50 - 100, 200 - 500, 1000 - 2000).sum());
		this.expectedReadSize = new SizeInBytes(itemSize.getValue().get() - 256);
		try {
			Files.delete(Paths.get(HOST_ITEM_OUTPUT_FILE));
		} catch (final Exception ignored) {}
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
		env.add("COUNT_LIMIT=" + EXPECTED_COUNT);
		env.add("STEP_ID_UPDATE=" + stepIdUpdate);
		env.add("STEP_ID_READ=" + stepIdRead);
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
			args.add("--item-output-path=" + containerItemOutputPath);
			try {
				DirWithManyFilesDeleter.deleteExternal(hostItemOutputPath);
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
		testContainer.await(TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
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
		// UPDATE
		final Consumer<CSVRecord> ioTraceRecFuncUpdate = ioTraceRec -> {
			testOpTraceRecord(ioTraceRec, OpType.UPDATE.ordinal(), expectedUpdateSize);
			ioTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepIdUpdate, ioTraceRecFuncUpdate);
		assertEquals(
						"There should be " + EXPECTED_COUNT + " records in the I/O trace log file",
						EXPECTED_COUNT,
						ioTraceRecCount.sum());
		// READ
		ioTraceRecCount.reset();
		final Consumer<CSVRecord> ioTraceRecFuncRead = ioTraceRec -> {
			testOpTraceRecord(ioTraceRec, OpType.READ.ordinal(), expectedReadSize);
			ioTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepIdRead, ioTraceRecFuncRead);
		assertEquals(
						"There should be " + EXPECTED_COUNT + " records in the I/O trace log file",
						EXPECTED_COUNT,
						ioTraceRecCount.sum());
		// TOTAL
		final List<CSVRecord> totalMetrcisLogRecordsUpdate = getMetricsTotalLogRecords(stepIdUpdate);
		testTotalMetricsLogRecord(
						totalMetrcisLogRecordsUpdate.get(0),
						OpType.UPDATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedUpdateSize,
						EXPECTED_COUNT,
						0,
						0);
		final List<CSVRecord> totalMetrcisLogRecordsRead = getMetricsTotalLogRecords(stepIdRead);
		testTotalMetricsLogRecord(
						totalMetrcisLogRecordsRead.get(0),
						OpType.READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedReadSize,
						EXPECTED_COUNT,
						0,
						0);
		final List<CSVRecord> updateMetricsRecords = getMetricsLogRecords(stepIdUpdate);
		final List<CSVRecord> readMetricsRecords = getMetricsLogRecords(stepIdRead);
		testMetricsLogRecords(
						updateMetricsRecords,
						OpType.UPDATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedUpdateSize,
						EXPECTED_COUNT,
						0,
						averagePeriod);
		testMetricsLogRecords(
						readMetricsRecords,
						OpType.READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedReadSize,
						EXPECTED_COUNT,
						0,
						averagePeriod);
		testFinalMetricsStdout(
						stdOutContent,
						OpType.UPDATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedUpdateSize,
						stepIdUpdate);
		testFinalMetricsStdout(
						stdOutContent,
						OpType.READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedReadSize,
						stepIdRead);
	}
}
