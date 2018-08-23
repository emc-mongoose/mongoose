package com.emc.mongoose.system;

import com.emc.mongoose.config.BundledDefaultsProvider;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.EnvParams;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;
import com.emc.mongoose.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseAdditionalNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.system.util.LogValidationUtil.testOpTraceLogRecords;
import static com.emc.mongoose.system.util.TestCaseUtil.stepId;
import static com.emc.mongoose.system.util.docker.MongooseContainer.CONTAINER_SHARE_PATH;
import static com.emc.mongoose.system.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.system.util.docker.MongooseContainer.containerScenarioPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class) public class ReadCustomContentVerificationFailTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private final String SCENARIO_PATH = containerScenarioPath(getClass());
	private final int timeoutInMillis = 1000_000;
	private final String ITEM_LIST_FILE = CONTAINER_SHARE_PATH + "/example/content/textexample";
	private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
	private final Map<String, MongooseAdditionalNodeContainer> slaveNodes = new HashMap<>();
	private final MongooseContainer testContainer;
	private final String stepId;
	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;
	private final Config config;
	private final String hostItemOutputPath = MongooseContainer.getHostItemOutputPath(getClass().getSimpleName());
	private final String hostItemOutputFile =
		HOST_SHARE_PATH + "/" + CreateLimitBySizeTest.class.getSimpleName() + ".csv";
	private String stdOutContent = null;

	public ReadCustomContentVerificationFailTest(
		final StorageType storageType, final RunMode runMode, final Concurrency concurrency, final ItemSize itemSize
	)
	throws Exception {
		final Map<String, Object> schema =
			SchemaProvider.resolveAndReduce(APP_NAME, Thread.currentThread().getContextClassLoader());
		config = new BundledDefaultsProvider().config(ARG_PATH_SEP, schema);
		stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
		try {
			FileUtils.deleteDirectory(Paths.get(MongooseContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch(final IOException ignored) {
		}
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
		try {
			Files.delete(Paths.get(hostItemOutputFile));
		} catch(final Exception ignored) {
		}
		final List<String> env = System
			.getenv()
			.entrySet()
			.stream()
			.map(e -> e.getKey() + "=" + e.getValue())
			.collect(Collectors.toList());
		final List<String> args = new ArrayList<>();
		args.add("--item-data-input-file=" + ITEM_LIST_FILE);
		switch(storageType) {
			case ATMOS:
			case S3:
			case SWIFT:
				final HttpStorageMockContainer storageMock = new HttpStorageMockContainer(HttpStorageMockContainer.DEFAULT_PORT, false, null, null,
					Character.MAX_RADIX, HttpStorageMockContainer.DEFAULT_CAPACITY,
					HttpStorageMockContainer.DEFAULT_CONTAINER_CAPACITY,
					HttpStorageMockContainer.DEFAULT_CONTAINER_COUNT_LIMIT,
					HttpStorageMockContainer.DEFAULT_FAIL_CONNECT_EVERY,
					HttpStorageMockContainer.DEFAULT_FAIL_RESPONSES_EVERY, 0
				);
				final String addr = "127.0.0.1:" + HttpStorageMockContainer.DEFAULT_PORT;
				storageMocks.put(addr, storageMock);
				args.add("--storage-net-node-addrs=" + storageMocks.keySet().stream().collect(Collectors.joining(",")));
				break;
			case FS:
				args.add("--item-output-path=" + hostItemOutputPath);
				try {
					DirWithManyFilesDeleter.deleteExternal(hostItemOutputPath);
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
				break;
		}
		switch(runMode) {
			case DISTRIBUTED:
				for(int i = 1; i < runMode.getNodeCount(); i++) {
					final int port = MongooseAdditionalNodeContainer.DEFAULT_PORT + i;
					final MongooseAdditionalNodeContainer nodeSvc = new MongooseAdditionalNodeContainer(port);
					final String addr = "127.0.0.1:" + port;
					slaveNodes.put(addr, nodeSvc);
				}
				args.add("--load-step-node-addrs=" + slaveNodes.keySet().stream().collect(Collectors.joining(",")));
				break;
		}
		testContainer = new MongooseContainer(
			stepId, storageType, runMode, concurrency, itemSize, SCENARIO_PATH, env, args
		);
	}

	@Before
	public final void setUp()
	throws Exception {
		storageMocks.values().forEach(AsyncRunnableBase::start);
		slaveNodes.values().forEach(AsyncRunnableBase::start);
		testContainer.start();
		testContainer.await(timeoutInMillis, TimeUnit.MILLISECONDS);
		stdOutContent = testContainer.stdOutContent();
	}

	@After
	public final void tearDown()
	throws Exception {
		testContainer.close();
		slaveNodes.values().parallelStream().forEach(storageMock -> {
			try {
				storageMock.close();
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			}
		});
		storageMocks.values().parallelStream().forEach(storageMock -> {
			try {
				storageMock.close();
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			}
		});
	}

	@Test
	public final void test()
	throws Exception {
		final LongAdder ioTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceRecTestFunc = ioTraceRec -> {
			assertEquals(
				"Record #" + ioTraceRecCount.sum() + ": unexpected operation type " + ioTraceRec.get(2),
				OpType.READ, OpType.values()[Integer.parseInt(ioTraceRec.get(2))]
			);
			assertEquals(
				"Record #" + ioTraceRecCount.sum() + ": unexpected status code " + ioTraceRec.get(3),
				Operation.Status.RESP_FAIL_CORRUPT, Operation.Status.values()[Integer.parseInt(ioTraceRec.get(3))]
			);
			ioTraceRecCount.increment();
		};
		testOpTraceLogRecords(stepId, ioTraceRecTestFunc);
		assertTrue("The count of the I/O trace records should be > 0", ioTraceRecCount.sum() > 0);
	}
}
