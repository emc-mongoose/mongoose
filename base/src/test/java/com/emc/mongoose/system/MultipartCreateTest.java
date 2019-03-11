package com.emc.mongoose.system;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static com.emc.mongoose.util.LogValidationUtil.getMetricsTotalLogRecords;
import static com.emc.mongoose.util.LogValidationUtil.testFinalMetricsStdout;
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

import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.params.Concurrency;
import com.emc.mongoose.params.EnvParams;
import com.emc.mongoose.params.ItemSize;
import com.emc.mongoose.params.RunMode;
import com.emc.mongoose.params.StorageType;
import com.emc.mongoose.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.util.docker.MongooseAdditionalNodeContainer;
import com.emc.mongoose.util.docker.MongooseEntryNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.system.SizeInBytes;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MultipartCreateTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = systemTestContainerScenarioPath(getClass());
	private final int TIMEOUT_IN_MILLIS = 1_000_000;
	private final String ITEM_OUTPUT_FILE = snakeCaseName(getClass()) + "_items.csv";
	private final String HOST_ITEM_OUTPUT_FILE = HOST_SHARE_PATH + File.separator + ITEM_OUTPUT_FILE;
	private final String CONTAINER_ITEM_OUTPUT_FILE = CONTAINER_SHARE_PATH + "/" + ITEM_OUTPUT_FILE;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final SizeInBytes partSize;
	private final SizeInBytes fullItemSize;
	private final SizeInBytes sizeLimit;
	private final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");
	private String stdOutContent = null;
	private long expectedCountMax;

	public MultipartCreateTest(
					final StorageType storageType,
					final RunMode runMode,
					final Concurrency concurrency,
					final ItemSize itemSize)
					throws Exception {
		partSize = itemSize.getValue();
		fullItemSize = new SizeInBytes((long) (1.2 * partSize.get()), 34 * partSize.get(), 2);
		Loggers.MSG.info("Item size: {}, part size: {}", fullItemSize, partSize);
		sizeLimit = new SizeInBytes(
						Math.min(
										SizeInBytes.toFixedSize("100GB"),
										5 * concurrency.getValue() * fullItemSize.getAvg()));
		Loggers.MSG.info("Use the size limit: {}", sizeLimit);
		expectedCountMax = sizeLimit.get() / fullItemSize.getMin();
		final Map<String, Object> schema = SchemaProvider.resolveAndReduce(APP_NAME, Thread.currentThread().getContextClassLoader());
		stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		try {
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch (final IOException ignored) {}
		this.runMode = runMode;
		this.concurrency = concurrency;
		try {
			Files.delete(Paths.get(HOST_ITEM_OUTPUT_FILE));
		} catch (final Exception ignored) {}
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
		env.add("PART_SIZE=" + partSize.toString());
		env.add("ITEM_OUTPUT_FILE=" + CONTAINER_ITEM_OUTPUT_FILE);
		env.add("SIZE_LIMIT=" + sizeLimit.toString());
		final List<String> args = new ArrayList<>();
		// args.add("--item-data-size=" + fullItemSize);
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
						stepId, storageType, runMode, concurrency, fullItemSize, SCENARIO_PATH, env, args);
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
		final LongAdder opTraceRecCount = new LongAdder();
		final SizeInBytes ZERO_SIZE = new SizeInBytes(0);
		final SizeInBytes TAIL_PART_SIZE = new SizeInBytes(1, partSize.get(), 1);
		final Consumer<CSVRecord> opTraceRecFunc = ioTraceRec -> {
			try {
				testOpTraceRecord(ioTraceRec, OpType.CREATE.ordinal(), ZERO_SIZE);
			} catch (final AssertionError e) {
				try {
					testOpTraceRecord(ioTraceRec, OpType.CREATE.ordinal(), partSize);
				} catch (final AssertionError ee) {
					testOpTraceRecord(ioTraceRec, OpType.CREATE.ordinal(), TAIL_PART_SIZE);
				}
			}
			opTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepId, opTraceRecFunc);

		final List<CSVRecord> itemRecs = new ArrayList<>();
		try (final BufferedReader br = new BufferedReader(new FileReader(HOST_ITEM_OUTPUT_FILE))) {
			try (final CSVParser csvParser = CSVFormat.RFC4180.parse(br)) {
				for (final CSVRecord csvRecord : csvParser) {
					itemRecs.add(csvRecord);
				}
			}
		}
		long nextItemSize;
		long sizeSum = 0;
		final int n = itemRecs.size();
		assertTrue(n > 0);
		assertTrue(
						"Expected no more than " + expectedCountMax + " items, but got " + n,
						expectedCountMax >= n);
		for (final CSVRecord itemRec : itemRecs) {
			nextItemSize = Long.parseLong(itemRec.get(2));
			assertTrue(fullItemSize.getMin() <= nextItemSize);
			assertTrue(fullItemSize.getMax() >= nextItemSize);
			sizeSum += nextItemSize;
		}
		final long delta = sizeLimit.get() / 5;
		assertTrue(
						"Expected to transfer no more than "
										+ sizeLimit
										+ "+"
										+ delta
										+ ", but transferred actually: "
										+ new SizeInBytes(sizeSum),
						sizeLimit.get() + delta >= sizeSum);

		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords(stepId);
		assertEquals(
						"There should be 1 total metrics records in the log file",
						1,
						totalMetrcisLogRecords.size());
		testTotalMetricsLogRecord(
						totalMetrcisLogRecords.get(0),
						OpType.CREATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						fullItemSize,
						0,
						1,
						0);
		testFinalMetricsStdout(
						stdOutContent,
						OpType.CREATE,
						concurrency.getValue(),
						runMode.getNodeCount(),
						fullItemSize,
						stepId);
	}
}
