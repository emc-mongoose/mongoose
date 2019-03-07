package com.emc.mongoose.system;

import static com.emc.mongoose.util.LogValidationUtil.testMetricsTableStdout;
import static com.emc.mongoose.util.TestCaseUtil.stepId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.params.Concurrency;
import com.emc.mongoose.params.EnvParams;
import com.emc.mongoose.params.ItemSize;
import com.emc.mongoose.params.RunMode;
import com.emc.mongoose.params.StorageType;
import com.emc.mongoose.util.DirWithManyFilesDeleter;
import com.emc.mongoose.util.OpenFilesCounter;
import com.emc.mongoose.util.PortTools;
import com.emc.mongoose.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.util.docker.MongooseAdditionalNodeContainer;
import com.emc.mongoose.util.docker.MongooseEntryNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class UnlimitedCreateTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = null; // default
	private final String containerItemOutputPath = MongooseEntryNodeContainer.getContainerItemOutputPath(getClass().getSimpleName());
	private final String hostItemOutputPath = MongooseEntryNodeContainer.getHostItemOutputPath(getClass().getSimpleName());
	private static final int TIMEOUT_IN_MILLIS = 60_000;
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;

	public UnlimitedCreateTest(
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
		final List<String> env = System.getenv().entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.toList());
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
	}

	@Test
	public final void test() throws Exception {
		final String stdOutContent = testContainer.stdOutContent();
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
		final int expectedConcurrency = runMode.getNodeCount() * concurrency.getValue();
		if (storageType.equals(StorageType.FS)) {
			// because of the following line the test is valid only in 'run_mode = local'
			final int actualConcurrency = OpenFilesCounter.getOpenFilesCount(
							MongooseEntryNodeContainer.getHostItemOutputPath(stepId));
			assertTrue(
							"Expected concurrency <= " + actualConcurrency + ", actual: " + actualConcurrency,
							actualConcurrency <= expectedConcurrency);
		} else {
			int actualConcurrency = 0;
			final int startPort = HttpStorageMockContainer.DEFAULT_PORT;
			for (int j = 0; j < runMode.getNodeCount(); ++j) {
				actualConcurrency += PortTools.getConnectionCount("127.0.0.1:" + (startPort + j));
			}
			assertEquals(
							"Expected concurrency: " + actualConcurrency + ", actual: " + actualConcurrency,
							expectedConcurrency,
							actualConcurrency,
							expectedConcurrency / 100);
		}
	}
}
