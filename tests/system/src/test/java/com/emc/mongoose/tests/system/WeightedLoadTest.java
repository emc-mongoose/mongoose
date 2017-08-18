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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

/**
 Created by andrey on 27.03.17.
 Covered use cases:
 * 2.1.1.1.2. Small Data Items (1B-100KB)
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 4.3. Medium Concurrency Level (11-100)
 * 5. Circularity
 * 6.2.2. Limit Step by Processed Item Count
 * 6.2.5. Limit Step by Time
 * 7.1. Metrics Periodic Reporting
 * 7.2. Metrics Reporting is Suppressed for the Precondition Steps
 * 8.2.1. Create New Items
 * 8.3.1. Read With Disabled Validation
 * 9.1. Scenarios Syntax
 * 9.3. Custom Scenario File
 * 9.4.1. Override Default Configuration in the Scenario
 * 9.4.2. Step Configuration Inheritance
 * 9.4.3. Reusing The Items in the Scenario
 * 9.5.7.2. Weighted Load Step
 * 10.1.2. Many Local Separate Storage Driver Services (at different ports)
 */

public class WeightedLoadTest
extends ScenarioTestBase {

	private static boolean FINISHED_IN_TIME;
	private static String STD_OUTPUT;
	private static int ACTUAL_CONCURRENCY;
	private static String itemOutputPath;

	public WeightedLoadTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "WeightedLoad.json");
	}

	@Override
	protected String makeStepId() {
		return WeightedLoadTest.class.getSimpleName() + '-' + storageType.name() + '-' +
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
					stdOutStream.startRecording();
					scenario.run();
					STD_OUTPUT = stdOutStream.stopRecordingAndGet();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.SECONDS.sleep(20); // warmup
		switch(storageType) {
			case FS:
				ACTUAL_CONCURRENCY = OpenFilesCounter.getOpenFilesCount(itemOutputPath);
				break;
			case ATMOS:
			case S3:
			case SWIFT:
				final int startPort = config.getStorageConfig().getNetConfig().getNodeConfig().getPort();
				for(int i = 0; i < httpStorageNodeCount; i ++) {
					ACTUAL_CONCURRENCY += PortTools.getConnectionCount("127.0.0.1:" + (startPort + i));
				}
				break;
		}
		TimeUnit.SECONDS.timedJoin(runner, 60);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
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
		final Map<IoType, Integer> weightsMap = new HashMap<>();
		testMetricsTableStdout(STD_OUTPUT, stepId, driverCount.getValue(), 0, concurrencyMap);

		assertTrue("Scenario didn't finished in time", FINISHED_IN_TIME);

		if(!StorageType.FS.equals(storageType)) {
			assertEquals(2 * driverCount.getValue() * concurrency.getValue(), ACTUAL_CONCURRENCY, 5);
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
