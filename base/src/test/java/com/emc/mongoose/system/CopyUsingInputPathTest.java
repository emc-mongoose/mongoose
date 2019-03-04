package com.emc.mongoose.system;

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

import com.emc.mongoose.base.config.TimeUtil;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.params.Concurrency;
import com.emc.mongoose.params.EnvParams;
import com.emc.mongoose.params.ItemSize;
import com.emc.mongoose.params.RunMode;
import com.emc.mongoose.params.StorageType;
import com.emc.mongoose.util.DirWithManyFilesDeleter;
import com.emc.mongoose.util.HttpStorageMockUtil;
import com.emc.mongoose.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.util.docker.MongooseAdditionalNodeContainer;
import com.emc.mongoose.util.docker.MongooseEntryNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
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
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public final class CopyUsingInputPathTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = systemTestContainerScenarioPath(getClass());
	private static final int COUNT_LIMIT = 100_000;
	private final int TIMEOUT_IN_MILLIS = 105_000;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;
	private final String itemSrcPath;
	private final String itemDstPath;

	public CopyUsingInputPathTest(
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
		final String itemPathPrefix;
		if (storageType.equals(StorageType.FS)) {
			itemPathPrefix = CONTAINER_SHARE_PATH + "/" + stepId;
		} else {
			itemPathPrefix = '/' + stepId;
		}
		itemDstPath = itemPathPrefix + "-Dst";
		itemSrcPath = itemPathPrefix + "-Src";
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
		env.add("ITEM_SRC_PATH=" + itemSrcPath);
		env.add("ITEM_DST_PATH=" + itemDstPath);
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
			try {
				DirWithManyFilesDeleter.deleteExternal(
								itemSrcPath.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString()));
			} catch (final Throwable ignored) {}
			try {
				DirWithManyFilesDeleter.deleteExternal(
								itemDstPath.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString()));
			} catch (final Throwable ignored) {}
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
		try {
			DirWithManyFilesDeleter.deleteExternal(
							itemSrcPath.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString()));
		} catch (final Throwable ignored) {}
		try {
			DirWithManyFilesDeleter.deleteExternal(
							itemDstPath.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString()));
		} catch (final Throwable ignored) {}
	}

	@Test
	public final void test() throws Exception {
		final LongAdder opTraceRecCount = new LongAdder();
		final LongAdder lostItemsCount = new LongAdder();
		final Consumer<CSVRecord> opTraceRecTestFunc;
		if (storageType.equals(StorageType.FS)) {
			final String hostItemSrcPath = itemSrcPath.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString());
			final String hostItemDstPath = itemDstPath.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString());
			opTraceRecTestFunc = opTraceRecord -> {
				File nextSrcFile;
				File nextDstFile;
				final String nextItemPath = opTraceRecord.get(1);
				final String nextItemId = nextItemPath.substring(nextItemPath.lastIndexOf('/') + 1);
				nextSrcFile = Paths.get(hostItemSrcPath, nextItemId).toFile();
				nextDstFile = Paths.get(hostItemDstPath, nextItemId).toFile();
				assertTrue("File \"" + nextItemPath + "\" doesn't exist", nextSrcFile.exists());
				if (!nextDstFile.exists()) {
					lostItemsCount.increment();
				} else {
					assertEquals(
									"Source file ("
													+ nextItemPath
													+ ") size ("
													+ nextSrcFile.length()
													+ " is not equal to the destination file ("
													+ nextDstFile.getAbsolutePath()
													+ ") size ("
													+ nextDstFile.length(),
									nextSrcFile.length(),
									nextDstFile.length());
				}
				testOpTraceRecord(
								opTraceRecord, OpType.CREATE.ordinal(), new SizeInBytes(nextSrcFile.length()));
				opTraceRecCount.increment();
			};
		} else {
			final String node = storageMocks.keySet().iterator().next();
			opTraceRecTestFunc = opTraceRecord -> {
				testOpTraceRecord(opTraceRecord, OpType.CREATE.ordinal(), itemSize.getValue());
				final String nextItemPath = opTraceRecord.get(1);
				if (HttpStorageMockUtil.getContentLength(node, nextItemPath) < 0) {
					// not found
					lostItemsCount.increment();
				}
				final String nextItemId = nextItemPath.substring(nextItemPath.lastIndexOf('/') + 1);
				HttpStorageMockUtil.assertItemExists(
								node, itemSrcPath + '/' + nextItemId, itemSize.getValue().get());
				opTraceRecCount.increment();
			};
		}
		testOpTraceLogRecords(stepId, opTraceRecTestFunc);
		assertTrue(
						"There should be "
										+ COUNT_LIMIT
										+ " records in the I/O trace log file but got "
										+ opTraceRecCount.sum(),
						opTraceRecCount.sum() <= COUNT_LIMIT);
		assertEquals(0, lostItemsCount.sum(), COUNT_LIMIT / 10_000);
		final List<CSVRecord> totalMetricsLogRecords = getMetricsTotalLogRecords(stepId);
		assertEquals(
						"There should be 1 total metrics records in the log file",
						1,
						totalMetricsLogRecords.size());
		if (storageType.equals(StorageType.FS)) {
			// some files may remain not written fully
			testTotalMetricsLogRecord(
							totalMetricsLogRecords.get(0),
							OpType.CREATE,
							concurrency.getValue(),
							runMode.getNodeCount(),
							new SizeInBytes(itemSize.getValue().get() / 2, itemSize.getValue().get(), 1),
							0,
							0,
							0);
		} else {
			testTotalMetricsLogRecord(
							totalMetricsLogRecords.get(0),
							OpType.CREATE,
							concurrency.getValue(),
							runMode.getNodeCount(),
							itemSize.getValue(),
							0,
							0,
							0);
		}
		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords(stepId);
		assertTrue(
						"There should be more than 0 metrics records in the log file",
						metricsLogRecords.size() > 0);
		final int outputMetricsAveragePeriod;
		final Object outputMetricsAveragePeriodRaw = BUNDLED_DEFAULTS.val("output-metrics-average-period");
		if (outputMetricsAveragePeriodRaw instanceof String) {
			outputMetricsAveragePeriod = (int) TimeUtil.getTimeInSeconds((String) outputMetricsAveragePeriodRaw);
		} else {
			outputMetricsAveragePeriod = TypeUtil.typeConvert(outputMetricsAveragePeriodRaw, int.class);
		}
		if (storageType.equals(StorageType.FS)) {
			// some files may remain not written fully
			testMetricsLogRecords(
							metricsLogRecords,
							OpType.CREATE,
							concurrency.getValue(),
							runMode.getNodeCount(),
							new SizeInBytes(itemSize.getValue().get() / 2, itemSize.getValue().get(), 1),
							0,
							0,
							outputMetricsAveragePeriod);
		} else {
			testMetricsLogRecords(
							metricsLogRecords,
							OpType.CREATE,
							concurrency.getValue(),
							runMode.getNodeCount(),
							itemSize.getValue(),
							0,
							0,
							outputMetricsAveragePeriod);
		}
		final String stdOutContent = testContainer.stdOutContent();
		testFinalMetricsStdout(
						stdOutContent,
						OpType.CREATE,
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
						0,
						0,
						itemSize.getValue());
	}
}
