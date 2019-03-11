package com.emc.mongoose.system;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static com.emc.mongoose.base.Constants.M;
import static com.emc.mongoose.util.LogValidationUtil.testMetricsTableStdout;
import static com.emc.mongoose.util.LogValidationUtil.testOpTraceLogRecords;
import static com.emc.mongoose.util.TestCaseUtil.stepId;
import static com.emc.mongoose.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.getHostItemOutputPath;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.systemTestContainerScenarioPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import com.github.akurilov.commons.net.NetUtil;
import com.github.akurilov.confuse.SchemaProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
public class PipelineWithDelayTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = systemTestContainerScenarioPath(getClass());
	private final int DELAY_SECONDS = 20;
	private final int TIME_LIMIT = 60;
	private final String HOST_ITEM_OUTPUT_FILE = HOST_SHARE_PATH + "/" + getClass().getSimpleName() + ".csv";
	private final String zone1Addr = "127.0.0.1";
	private final String zone2Addr;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private boolean finishedInTime;
	private int containerExitCode;
	private String stdOutContent = null;

	public PipelineWithDelayTest(
					final StorageType storageType,
					final RunMode runMode,
					final Concurrency concurrency,
					final ItemSize itemSize)
					throws Exception {
		final Map<String, Object> schema = SchemaProvider.resolveAndReduce(APP_NAME, Thread.currentThread().getContextClassLoader());
		stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		try {
			FileUtils.deleteDirectory(
							Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch (final IOException ignored) {}
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		if (storageType.equals(StorageType.FS)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(getHostItemOutputPath(stepId));
			} catch (final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		try {
			Files.delete(Paths.get(HOST_ITEM_OUTPUT_FILE));
		} catch (final Exception ignored) {}
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
		try {
			zone2Addr = NetUtil.getHostAddrString();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		env.add("ZONE1_ADDRS=" + zone1Addr + ":" + HttpStorageMockContainer.DEFAULT_PORT);
		env.add("ZONE2_ADDRS=" + zone2Addr + ":" + HttpStorageMockContainer.DEFAULT_PORT);
		final List<String> args = new ArrayList<>();
		args.add("--storage-namespace=ns1");
		args.add("--load-step-limit-time=" + TIME_LIMIT);
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
	public final void setUp() {
		storageMocks.values().forEach(AsyncRunnableBase::start);
		slaveNodes.values().forEach(AsyncRunnableBase::start);
		long duration = System.currentTimeMillis();
		testContainer.start();
		System.out.println(
						"Test container await(120[s]) returned: " + testContainer.await(3, TimeUnit.MINUTES));
		stdOutContent = testContainer.stdOutContent();
		duration = System.currentTimeMillis() - duration;
		finishedInTime = (TimeUnit.MILLISECONDS.toMinutes(duration) <= 3);
		containerExitCode = testContainer.exitStatusCode();
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
		assertEquals("Container exit code should be 0", 0, containerExitCode);
		testMetricsTableStdout(
						stdOutContent,
						stepId,
						storageType,
						runMode.getNodeCount(),
						0,
						new HashMap<OpType, Integer>() {
							{
								put(OpType.CREATE, concurrency.getValue());
								put(OpType.READ, concurrency.getValue());
							}
						});
		final Map<String, Long> timingMap = new HashMap<>();
		final Consumer<CSVRecord> ioTraceRecTestFunc = new Consumer<CSVRecord>() {
			String storageNode;
			String itemPath;
			OpType opType;
			long reqTimeStart;
			long duration;
			Long prevOpFinishTime;

			@Override
			public final void accept(final CSVRecord ioTraceRec) {
				storageNode = ioTraceRec.get(0);
				itemPath = ioTraceRec.get(1);
				opType = OpType.values()[Integer.parseInt(ioTraceRec.get(2))];
				reqTimeStart = Long.parseLong(ioTraceRec.get(4));
				duration = Long.parseLong(ioTraceRec.get(5));
				switch (opType) {
				case CREATE:
					assertTrue(storageNode.startsWith(zone1Addr));
					timingMap.put(itemPath, reqTimeStart + duration);
					break;
				case READ:
					assertTrue(storageNode.startsWith(zone2Addr));
					prevOpFinishTime = timingMap.get(itemPath);
					if (prevOpFinishTime == null) {
						fail("No create I/O trace record for \"" + itemPath + "\"");
					} else {
						assertTrue((reqTimeStart - prevOpFinishTime) / M > DELAY_SECONDS);
					}
					break;
				default:
					fail("Unexpected I/O type: " + opType);
				}
			}
		};
		testOpTraceLogRecords(stepId, ioTraceRecTestFunc);
		assertTrue("Scenario didn't finished in time", finishedInTime);
	}
}
