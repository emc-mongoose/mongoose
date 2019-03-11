package com.emc.mongoose.system;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static com.emc.mongoose.base.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.base.item.op.OpType.READ;
import static com.emc.mongoose.base.item.op.OpType.UPDATE;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsStdout;
import static com.emc.mongoose.util.LogValidationUtil.testMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceRecord;
import static com.emc.mongoose.util.LogValidationUtil.testTotalMetricsLogRecord;
import static com.emc.mongoose.util.TestCaseUtil.stepId;
import static com.emc.mongoose.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.systemTestContainerScenarioPath;
import static org.junit.Assert.assertEquals;

import com.emc.mongoose.base.config.BundledDefaultsProvider;
import com.emc.mongoose.base.config.TimeUtil;
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
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SingleFixedUpdateAndSingleRandomReadTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private static final long EXPECTED_COUNT = 1_000;
	private static final int TIMEOUT_IN_MILLIS = 1_000_000;

	private final String scenarioPath = systemTestContainerScenarioPath(getClass());
	private final String containerItemOutputPath = MongooseEntryNodeContainer.getContainerItemOutputPath(getClass().getSimpleName());
	private final String hostItemOutputPath = MongooseEntryNodeContainer.getHostItemOutputPath(getClass().getSimpleName());
	private final String hostItemOutputFile = HOST_SHARE_PATH + "/" + getClass().getSimpleName() + ".csv";
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepIdUpdate;
	private final String stepIdRead;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final SizeInBytes expectedUpdateSize;
	private final SizeInBytes expectedReadSize;
	private final int averagePeriod;
	private final Config config;
	private String stdOutContent = null;

	public SingleFixedUpdateAndSingleRandomReadTest(
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
		final String stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		stepIdUpdate = stepId + "_Update";
		stepIdRead = stepId + "_Read";
		try {
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepIdUpdate).toFile());
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepIdRead).toFile());
		} catch (final IOException ignored) {}
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.expectedUpdateSize = new SizeInBytes(SizeInBytes.toFixedSize("5KB") - SizeInBytes.toFixedSize("2KB"));
		this.expectedReadSize = new SizeInBytes(1, itemSize.getValue().get(), 1);
		try {
			Files.delete(Paths.get(hostItemOutputFile));
		} catch (final Exception ignored) {}
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
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
							Character.MAX_RADIX,
							HttpStorageMockContainer.DEFAULT_CAPACITY,
							HttpStorageMockContainer.DEFAULT_CONTAINER_CAPACITY,
							HttpStorageMockContainer.DEFAULT_CONTAINER_COUNT_LIMIT,
							HttpStorageMockContainer.DEFAULT_FAIL_CONNECT_EVERY,
							HttpStorageMockContainer.DEFAULT_FAIL_RESPONSES_EVERY,
							0);
			final String addr = "127.0.0.1:" + HttpStorageMockContainer.DEFAULT_PORT;
			storageMocks.put(addr, storageMock);
			args.add("--storage-net-node-addrs=" + String.join(",", storageMocks.keySet()));
			break;
		case FS:
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
			args.add("--load-step-node-addrs=" + String.join(",", slaveNodes.keySet()));
			break;
		}
		testContainer = new MongooseEntryNodeContainer(
						stepId,
						storageType,
						runMode,
						concurrency,
						itemSize.getValue(),
						scenarioPath,
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
		// I/O traces
		final LongAdder ioTraceRecCount = new LongAdder();

		final Consumer<CSVRecord> updateOpTraceTestFunc = ioTraceRec -> {
			testOpTraceRecord(ioTraceRec, UPDATE.ordinal(), expectedUpdateSize);
			ioTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepIdUpdate, updateOpTraceTestFunc);
		assertEquals(
						"There should be " + EXPECTED_COUNT + " records in the op trace log file for update",
						EXPECTED_COUNT,
						ioTraceRecCount.sum());
		final Consumer<CSVRecord> readOpTraceTestFunc = ioTraceRec -> {
			testOpTraceRecord(ioTraceRec, READ.ordinal(), expectedReadSize);
			ioTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepIdRead, readOpTraceTestFunc);
		assertEquals(
						"There should be "
										+ 2 * EXPECTED_COUNT
										+ " records in the update & read op trace log files",
						2 * EXPECTED_COUNT,
						ioTraceRecCount.sum());
		final List<CSVRecord> totalUpdateMetrcisLogRecords = getMetricsTotalLogRecords(stepIdUpdate);
		testTotalMetricsLogRecord(
						totalUpdateMetrcisLogRecords.get(0),
						UPDATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedUpdateSize,
						EXPECTED_COUNT,
						0,
						0);
		final List<CSVRecord> totalReadMetrcisLogRecords = getMetricsTotalLogRecords(stepIdRead);
		testTotalMetricsLogRecord(
						totalReadMetrcisLogRecords.get(0),
						READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedReadSize,
						EXPECTED_COUNT,
						0,
						0);
		final List<CSVRecord> updateMetricsRecords = getMetricsLogRecords(stepIdUpdate);
		testMetricsLogRecords(
						updateMetricsRecords,
						UPDATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedUpdateSize,
						EXPECTED_COUNT,
						0,
						averagePeriod);
		final List<CSVRecord> readMetricsRecords = getMetricsLogRecords(stepIdRead);
		testMetricsLogRecords(
						readMetricsRecords,
						READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedReadSize,
						EXPECTED_COUNT,
						0,
						averagePeriod);
		testFinalMetricsStdout(
						stdOutContent,
						UPDATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedUpdateSize,
						stepIdUpdate);
		testFinalMetricsStdout(
						stdOutContent,
						READ,
						concurrency.getValue(),
						runMode.getNodeCount(),
						expectedReadSize,
						stepIdRead);
	}
}
