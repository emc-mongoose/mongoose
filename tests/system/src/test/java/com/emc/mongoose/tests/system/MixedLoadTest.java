package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.tests.system.util.OpenFilesCounter;
import com.emc.mongoose.tests.system.util.PortTools;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 23.03.17.
 Covered Use Cases:
 * 2.1.1.1.3. Intermediate Size Data Items (100KB-10MB)
 * 2.3.2. Items Output File
 * 2.3.3.1. Constant Items Destination Path
 * 4.3. Medium Concurrency Level (11-100)
 * 5. Circularity
 * 6.2.5. Limit Load Job by Time
 * 7.1. Metrics Periodic Reporting
 * 8.2.1. Create New Items
 * 8.3.1. Read With Disabled Validation
 * 9.1. Scenarios Syntax
 * 9.4.1. Override Default Configuration in the Scenario
 * 9.4.3. Reusing The Items in the Scenario
 * 9.5.3. Precondition Load Job
 * 9.5.7.1. Separate Configuration in the Mixed Load Job
 * 10.1.2. Many Local Separate Storage Driver Services (at different ports)
 */

public class MixedLoadTest
extends ScenarioTestBase {

	private boolean finishedInTime;
	private String stdOutput;
	private int actualConcurrency;
	private String itemOutputPath;

	public MixedLoadTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "MixedLoad.json");
	}

	@Override
	protected String makeStepId() {
		return MixedLoadTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}
	
	@Before
	public void setUp()
	throws Exception {
		configArgs.add("--storage-net-http-namespace=ns1");
		configArgs.add("--storage-mock-capacity=10000000");
		super.setUp();
		if(storageType.equals(StorageType.FS)) {
			itemOutputPath = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
			).toString();
			EnvUtil.set("ITEM_OUTPUT_PATH", itemOutputPath);
		} else {
			itemOutputPath = "/default";
			EnvUtil.set("ITEM_OUTPUT_PATH", stepId);
		}
		config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
		scenario = new JsonScenario(config, scenarioPath.toFile());
		final Thread runner = new Thread(
			() -> {
				try {
					scenario.run();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.SECONDS.sleep(10);
		stdOutStream.startRecording();
		TimeUnit.SECONDS.sleep(10);

		switch(storageType) {
			case FS:
				actualConcurrency = OpenFilesCounter.getOpenFilesCount(itemOutputPath);
				break;
			case ATMOS:
			case S3:
			case SWIFT:
				final int startPort = config.getStorageConfig().getNetConfig().getNodeConfig().getPort();
				for(int i = 0; i < httpStorageNodeCount; i ++) {
					actualConcurrency += PortTools.getConnectionCount("127.0.0.1:" + (startPort + i));
				}
				break;
		}

		TimeUnit.SECONDS.timedJoin(runner, 50);
		finishedInTime = !runner.isAlive();
		runner.interrupt();
		stdOutput = stdOutStream.stopRecordingAndGet();
		LogUtil.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}
	
	@After
	public void tearDown()
	throws Exception {
		if(storageType.equals(StorageType.FS)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(itemOutputPath);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {
		final Map<IoType, Integer> concurrencyMap = new HashMap<>();
		concurrencyMap.put(IoType.CREATE, concurrency.getValue());
		concurrencyMap.put(IoType.READ, concurrency.getValue());
		testMetricsTableStdout(stdOutput, stepId, driverCount.getValue(), 0, concurrencyMap);
		assertTrue("Scenario didn't finished in time", finishedInTime);
		if(!StorageType.FS.equals(storageType)) {
			assertEquals(driverCount.getValue() * concurrency.getValue(), actualConcurrency, 5);
		}

		// check if all files/connections are closed after the test
		int openChannels = 0;
		switch(storageType) {
			case FS:
				openChannels = OpenFilesCounter.getOpenFilesCount(itemOutputPath);
				break;
			case ATMOS:
			case S3:
			case SWIFT:
				final int startPort = config.getStorageConfig().getNetConfig().getNodeConfig().getPort();
				for(int i = 0; i < httpStorageNodeCount; i ++) {
					openChannels += PortTools.getConnectionCount("127.0.0.1:" + (startPort + i));
				}
				break;
		}
		assertEquals(
			"Expected no open channels after the test but got " + openChannels, 0, openChannels
		);
	}
}
