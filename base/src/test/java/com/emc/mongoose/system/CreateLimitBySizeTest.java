package com.emc.mongoose.system;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static com.emc.mongoose.base.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsStdout;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsTableRowStdout;
import static com.emc.mongoose.util.LogValidationUtil.testMetricsLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testMetricsTableStdout;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceRecord;
import static com.emc.mongoose.util.LogValidationUtil.testTotalMetricsLogRecord;
import static com.emc.mongoose.util.TestCaseUtil.stepId;
import static com.emc.mongoose.util.docker.MongooseContainer.CONTAINER_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.BUNDLED_DEFAULTS;
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
import com.emc.mongoose.util.HttpStorageMockUtil;
import com.emc.mongoose.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.util.docker.MongooseAdditionalNodeContainer;
import com.emc.mongoose.util.docker.MongooseEntryNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import java.io.BufferedReader;
import java.io.File;
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
import org.apache.commons.math3.stat.Frequency;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CreateLimitBySizeTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = null; // default
	private final double REQUIRED_ACCURACY = 1;
	// 100% because #issue-1252 "The Size limit is violated" isn't resolved -> Mongoose doesn't stop
	// in time
	private final int TIMEOUT_IN_MILLIS = 1000_000;
	private final String CONTAINER_ITEM_OUTPUT_FILE;
	private final String HOST_ITEM_OUTPUT_FILE;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;
	private final Config config;
	private final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");
	private final SizeInBytes sizeLimit;
	private final long expectedCount;
	private final int averagePeriod;
	private long duration;
	private int containerExitCode;
	private String stdOutContent = null;

	public CreateLimitBySizeTest(
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
		CONTAINER_ITEM_OUTPUT_FILE = CONTAINER_SHARE_PATH + '/' + getClass().getSimpleName() + '_' + stepId + ".csv";
		HOST_ITEM_OUTPUT_FILE = HOST_SHARE_PATH + "/" + getClass().getSimpleName() + '_' + stepId + ".csv";
		try {
			Files.delete(Paths.get(HOST_ITEM_OUTPUT_FILE));
		} catch (final IOException ignore) {}
		try {
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch (final IOException ignored) {}
		try {
			FileUtils.deleteDirectory(Paths.get(HOST_SHARE_PATH.toString(), stepId).toFile());
		} catch (final IOException ignored) {}
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
		// set-up a sizeLimit depending on the itemSize
		final long itemSizeValue = itemSize.getValue().get();
		if (itemSizeValue > SizeInBytes.toFixedSize("1GB")) {
			sizeLimit = new SizeInBytes(100 * itemSizeValue);
		} else if (itemSizeValue > SizeInBytes.toFixedSize("1MB")) {
			sizeLimit = new SizeInBytes(1_000 * itemSizeValue);
		} else if (itemSizeValue > SizeInBytes.toFixedSize("10KB")) {
			sizeLimit = new SizeInBytes(10_000 * itemSizeValue);
		} else {
			sizeLimit = new SizeInBytes(100_000 * itemSizeValue);
		}
		expectedCount = sizeLimit.get() / itemSizeValue;
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
		final List<String> args = new ArrayList<>();
		args.add("--item-output-file=" + CONTAINER_ITEM_OUTPUT_FILE);
		args.add("--load-step-limit-size=" + sizeLimit);
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
			args.add(
							"--item-output-path=" + MongooseEntryNodeContainer.getContainerItemOutputPath(stepId));
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
				slaveNodes.put(addr, nodeSvc);
			}
			args.add(
							"--load-step-node-addrs="
											+ slaveNodes.keySet().stream().collect(Collectors.joining(",")));
			break;
		}
		// use default scenario
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
		duration = System.currentTimeMillis();
		testContainer.start();
		testContainer.await(TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
		duration = System.currentTimeMillis() - duration;
		containerExitCode = testContainer.exitStatusCode();
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
		try {
			DirWithManyFilesDeleter.deleteExternal(
							MongooseEntryNodeContainer.getHostItemOutputPath(stepId));
		} catch (final Throwable t) {
			Assert.fail(t.toString());
		}
	}

	@Test
	public final void test() throws Exception {
		assertEquals("Container exit code should be 0", 0, containerExitCode);
		final LongAdder opTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> opTraceRecFunc;
		final String hostItemOutputPath = MongooseEntryNodeContainer.getHostItemOutputPath(stepId);
		if (storageType.equals(StorageType.FS)) {
			opTraceRecFunc = opTraceRecord -> {
				File nextDstFile;
				final String nextItemPath = opTraceRecord.get(1);
				final String nextItemId = nextItemPath.substring(nextItemPath.lastIndexOf('/') + 1);
				nextDstFile = Paths.get(hostItemOutputPath, nextItemId).toFile();
				// FIXME: next line is commented because issue #1252 isn't resolved
				// assertTrue("File \"" + nextDstFile + "\" doesn't exist", nextDstFile.exists());
				assertTrue(
								"File ("
												+ nextItemPath
												+ ") size ("
												+ nextDstFile.length()
												+ " is not equal to the configured: "
												+ itemSize.getValue(),
								itemSize.getValue().get() >= nextDstFile.length());
				opTraceRecCount.increment();
			};
		} else {
			final String nodeAddr = storageMocks.keySet().iterator().next();
			opTraceRecFunc = opTraceRec -> {
				testOpTraceRecord(opTraceRec, OpType.CREATE.ordinal(), itemSize.getValue());
				HttpStorageMockUtil.assertItemExists(
								nodeAddr, opTraceRec.get(1), Long.parseLong(opTraceRec.get(8)));
				opTraceRecCount.increment();
			};
		}
		testOpTraceLogRecords(stepId, opTraceRecFunc);
		// 100% because #issue-1252 "The Size limit is violated" isn't resolved -> Mongoose doesn't stop
		// in time
		assertEquals(expectedCount, opTraceRecCount.sum(), REQUIRED_ACCURACY * expectedCount);
		final List<CSVRecord> items = new ArrayList<>();
		try (final BufferedReader br = new BufferedReader(new FileReader(HOST_ITEM_OUTPUT_FILE))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for (final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		// 100% because #issue-1252 "The Size limit is violated" isn't resolved -> Mongoose doesn't stop
		// in time
		assertEquals(expectedCount, items.size(), expectedCount * REQUIRED_ACCURACY);
		final Frequency freq = new Frequency();
		String itemPath, itemId;
		long itemOffset;
		long size;
		String modLayerAndMask;
		for (final CSVRecord itemRec : items) {
			itemPath = itemRec.get(0);
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			itemOffset = Long.parseLong(itemRec.get(1), 0x10);
			assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
			freq.addValue(itemOffset);
			size = Long.parseLong(itemRec.get(2));
			assertEquals(itemSize.getValue().get(), size);
			modLayerAndMask = itemRec.get(3);
			assertEquals("0/0", modLayerAndMask);
		}
		assertEquals(items.size(), freq.getUniqueCount());
		testTotalMetricsLogRecord(
						getMetricsTotalLogRecords(stepId).get(0),
						OpType.CREATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						0,
						1,
						0);
		testMetricsLogRecords(
						getMetricsLogRecords(stepId),
						OpType.CREATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						0,
						0,
						averagePeriod);
		testFinalMetricsStdout(
						stdOutContent,
						OpType.CREATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						itemSize.getValue(),
						stepId);
		testMetricsTableStdout(
						stdOutContent,
						stepId,
						storageType,
						runMode.getNodeCount(),
						0,
						new HashMap<OpType, Integer>() {
							{
								put(OpType.CREATE, concurrency.getValue());
							}
						});
		testFinalMetricsTableRowStdout(
						stdOutContent,
						stepId,
						OpType.CREATE,
						runMode.getNodeCount(),
						concurrency.getValue(),
						0,
						0,
						itemSize.getValue());
		assertTrue(duration < TIMEOUT_IN_MILLIS);
	}
}
