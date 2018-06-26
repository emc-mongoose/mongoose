package com.emc.mongoose.system;

import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.EnvParams;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;
import com.emc.mongoose.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.system.util.HttpStorageMockUtil;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseSlaveNodeContainer;
import static com.emc.mongoose.system.util.LogValidationUtil.getContainerMetricsLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.getContainerMetricsTotalLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.testContainerIoTraceLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.testFinalMetricsTableRowStdout;
import static com.emc.mongoose.system.util.LogValidationUtil.testIoTraceRecord;
import static com.emc.mongoose.system.util.LogValidationUtil.testMetricsLogRecords;
import static com.emc.mongoose.system.util.LogValidationUtil.testSingleMetricsStdout;
import static com.emc.mongoose.system.util.LogValidationUtil.testTotalMetricsLogRecord;
import static com.emc.mongoose.system.util.TestCaseUtil.stepId;
import static com.emc.mongoose.system.util.docker.MongooseContainer.BUNDLED_DEFAULTS;
import static com.emc.mongoose.system.util.docker.MongooseContainer.CONTAINER_SHARE_PATH;
import static com.emc.mongoose.system.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.system.util.docker.MongooseContainer.containerScenarioPath;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;

import org.apache.commons.csv.CSVRecord;

import org.apache.commons.io.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

@RunWith(Parameterized.class)
public final class CopyUsingInputPathTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private static final int COUNT_LIMIT = 100_000;

	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseSlaveNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;
	private final String itemSrcPath;
	private final String itemDstPath;

	public CopyUsingInputPathTest(
		final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {

		stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		try {
			FileUtils.deleteDirectory(MongooseContainer.HOST_LOG_PATH.toFile());
		} catch(final IOException ignored) {
		}

		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;

		final String itemPathPrefix;
		if(storageType.equals(StorageType.FS)) {
			itemPathPrefix = CONTAINER_SHARE_PATH + "/" + stepId;
		} else {
			itemPathPrefix = '/' + stepId;
		}
		itemDstPath = itemPathPrefix + "-Dst";
		itemSrcPath = itemPathPrefix + "-Src";
		if(storageType.equals(StorageType.FS)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(
					itemSrcPath.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString())
				);
			} catch(final Throwable ignored) {
			}
			try {
				DirWithManyFilesDeleter.deleteExternal(
					itemDstPath.replace(CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString())
				);
			} catch(final Throwable ignored) {
			}
		}

		final List<String> env = System.getenv()
			.entrySet()
			.stream()
			.map(e -> e.getKey() + "=" + e.getValue())
			.collect(Collectors.toList());
		env.add("ITEM_SRC_PATH=" + itemSrcPath);
		env.add("ITEM_DST_PATH=" + itemDstPath);

		final List<String> args = new ArrayList<>();

		switch(storageType) {
			case ATMOS:
			case S3:
			case SWIFT:
				final HttpStorageMockContainer storageMock = new HttpStorageMockContainer(
					HttpStorageMockContainer.DEFAULT_PORT, false, null, null, Character.MAX_RADIX,
					HttpStorageMockContainer.DEFAULT_CAPACITY,
					HttpStorageMockContainer.DEFAULT_CONTAINER_CAPACITY,
					HttpStorageMockContainer.DEFAULT_CONTAINER_COUNT_LIMIT,
					HttpStorageMockContainer.DEFAULT_FAIL_CONNECT_EVERY,
					HttpStorageMockContainer.DEFAULT_FAIL_RESPONSES_EVERY,
					0
				);
				final String addr = "127.0.0.1:" + HttpStorageMockContainer.DEFAULT_PORT;
				storageMocks.put(addr, storageMock);
				args.add(
					"--storage-net-node-addrs="
						+ storageMocks.keySet().stream().collect(Collectors.joining(","))
				);
				break;
		}

		switch(runMode) {
			case DISTRIBUTED:
				final String localExternalAddr = ServiceUtil.getAnyExternalHostAddress();
				args.add("--load-step-distributed");
				for(int i = 0; i < runMode.getNodeCount(); i ++) {
					final int port = MongooseSlaveNodeContainer.DEFAULT_PORT + i;
					final MongooseSlaveNodeContainer nodeSvc = new MongooseSlaveNodeContainer(port);
					final String addr = localExternalAddr + ":" + port;
					slaveNodes.put(addr, nodeSvc);
				}
				args.add(
					"--load-step-node-addrs="
						+ slaveNodes.keySet().stream().collect(Collectors.joining(","))
				);
				break;
		}

		final String containerScenarioPath = containerScenarioPath(getClass());
		testContainer = new MongooseContainer(
			stepId, storageType, runMode, concurrency, itemSize, containerScenarioPath, env, args
		);
	}

	@Before
	public final void setUp()
	throws Exception {
		storageMocks.values().forEach(AsyncRunnableBase::start);
		slaveNodes.values().forEach(AsyncRunnableBase::start);
		testContainer.start();
		testContainer.await(1000, TimeUnit.SECONDS);
	}

	@After
	public final void tearDown()
	throws Exception {

		testContainer.close();

		slaveNodes.values().parallelStream().forEach(
			storageMock -> {
				try {
					storageMock.close();
				} catch(final Throwable t) {
					t.printStackTrace(System.err);
				}
			}
		);
		storageMocks.values().parallelStream().forEach(
			storageMock -> {
				try {
					storageMock.close();
				} catch(final Throwable t) {
					t.printStackTrace(System.err);
				}
			}
		);
	}

	@Test
	public final void test()
	throws Exception {

		final LongAdder ioTraceRecCount = new LongAdder();
		final LongAdder lostItemsCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceRecTestFunc;
		if(storageType.equals(StorageType.FS)) {
			final String hostItemSrcPath = itemSrcPath.replace(
				CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString()
			);
			final String hostItemDstPath = itemDstPath.replace(
				CONTAINER_SHARE_PATH, HOST_SHARE_PATH.toString()
			);
			ioTraceRecTestFunc = ioTraceRecord -> {
				File nextSrcFile;
				File nextDstFile;
				final String nextItemPath = ioTraceRecord.get("ItemPath");
				final String nextItemId = nextItemPath.substring(
					nextItemPath.lastIndexOf(File.separatorChar) + 1
				);
				nextSrcFile = Paths.get(hostItemSrcPath, nextItemId).toFile();
				nextDstFile = Paths.get(hostItemDstPath, nextItemId).toFile();
				assertTrue(
					"File \"" + nextItemPath + "\" doesn't exist", nextSrcFile.exists()
				);
				if(!nextDstFile.exists()) {
					lostItemsCount.increment();
				} else {
					assertEquals(
						"Source file (" + nextItemPath + ") size (" + nextSrcFile.length() +
							" is not equal to the destination file (" + nextDstFile.getAbsolutePath() +
							") size (" + nextDstFile.length(),
						nextSrcFile.length(), nextDstFile.length()
					);
				}
				testIoTraceRecord(
					ioTraceRecord, IoType.CREATE.ordinal(), new SizeInBytes(nextSrcFile.length())
				);
				ioTraceRecCount.increment();
			};
		} else {
			final String node = storageMocks.keySet().iterator().next();
			ioTraceRecTestFunc = ioTraceRecord -> {
				testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), itemSize.getValue());
				final String nextItemPath = ioTraceRecord.get("ItemPath");
				if(HttpStorageMockUtil.getContentLength(node, nextItemPath) < 0) {
					// not found
					lostItemsCount.increment();
				}
				final String nextItemId = nextItemPath.substring(
					nextItemPath.lastIndexOf(File.separatorChar) + 1
				);
				HttpStorageMockUtil.assertItemExists(
					node, itemSrcPath + '/' + nextItemId, itemSize.getValue().get()
				);
				ioTraceRecCount.increment();
			};
		}
		testContainerIoTraceLogRecords(stepId, ioTraceRecTestFunc);

		assertTrue(
			"There should be " + COUNT_LIMIT + " records in the I/O trace log file but got "
				+ ioTraceRecCount.sum(),
			ioTraceRecCount.sum() <= COUNT_LIMIT
		);
		assertEquals(0, lostItemsCount.sum(), COUNT_LIMIT / 10_000);

		final List<CSVRecord> totalMetricsLogRecords = getContainerMetricsTotalLogRecords(stepId);
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetricsLogRecords.size()
		);
		if(storageType.equals(StorageType.FS)) {
			// some files may remain not written fully
			testTotalMetricsLogRecord(
				totalMetricsLogRecords.get(0), IoType.CREATE, concurrency.getValue(),
				runMode.getNodeCount(),
				new SizeInBytes(itemSize.getValue().get() / 2, itemSize.getValue().get(), 1), 0, 0
			);
		} else {
			testTotalMetricsLogRecord(
				totalMetricsLogRecords.get(0), IoType.CREATE, concurrency.getValue(),
				runMode.getNodeCount(), itemSize.getValue(), 0, 0
			);
		}

		final List<CSVRecord> metricsLogRecords = getContainerMetricsLogRecords(stepId);
		assertTrue(
			"There should be more than 0 metrics records in the log file",
			metricsLogRecords.size() > 0
		);
		final int outputMetricsAveragePeriod;
		final Object outputMetricsAveragePeriodRaw = BUNDLED_DEFAULTS.val("output-metrics-average-period");
		if(outputMetricsAveragePeriodRaw instanceof String) {
			outputMetricsAveragePeriod = (int) TimeUtil.getTimeInSeconds((String) outputMetricsAveragePeriodRaw);
		} else {
			outputMetricsAveragePeriod = TypeUtil.typeConvert(outputMetricsAveragePeriodRaw, int.class);
		}

		if(storageType.equals(StorageType.FS)) {
			// some files may remain not written fully
			testMetricsLogRecords(
				metricsLogRecords, IoType.CREATE, concurrency.getValue(), runMode.getNodeCount(),
				new SizeInBytes(itemSize.getValue().get() / 2, itemSize.getValue().get(), 1), 0, 0,
				outputMetricsAveragePeriod
			);
		} else {
			testMetricsLogRecords(
				metricsLogRecords, IoType.CREATE, concurrency.getValue(), runMode.getNodeCount(), itemSize.getValue(),
				0, 0, outputMetricsAveragePeriod
			);
		}

		final String stdOutContent = testContainer.stdOutContent();
		testSingleMetricsStdout(
			stdOutContent.replaceAll("[\r\n]+", " "), IoType.CREATE, concurrency.getValue(), runMode.getNodeCount(),
			itemSize.getValue(), outputMetricsAveragePeriod
		);
		testFinalMetricsTableRowStdout(
			stdOutContent, stepId, IoType.CREATE, runMode.getNodeCount(), concurrency.getValue(), 0, 0,
			itemSize.getValue()
		);
	}
}
