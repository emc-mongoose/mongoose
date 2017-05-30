package com.emc.mongoose.tests.system;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.tests.system.util.PortListener;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
extends HttpStorageDistributedScenarioTestBase {
	private static final Path SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, "mixed", "mixed.json"
	);
	private static final int EXPECTED_CONCURRENCY = 20 + 50;
	
	private static boolean FINISHED_IN_TIME;
	private static String STD_OUTPUT;
	private static int ACTUAL_CONCURRENCY;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = MixedLoadTest.class.getSimpleName();
		try {
			Files.delete(Paths.get("items2read.csv"));
		} catch(final Exception ignored) {
		}
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		CONFIG_ARGS.add("--test-scenario-file=" + SCENARIO_PATH.toString());
		HttpStorageDistributedScenarioTestBase.setUpClass();
		final Thread runner = new Thread(
			() -> {
				try {
					SCENARIO.run();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.SECONDS.sleep(10);
		STD_OUT_STREAM.startRecording();
		TimeUnit.SECONDS.sleep(10);
		final int startPort = CONFIG.getStorageConfig().getNetConfig().getNodeConfig().getPort();
		for(int i = 0; i < STORAGE_NODE_COUNT; i ++) {
			ACTUAL_CONCURRENCY += PortListener
				.getCountConnectionsOnPort("127.0.0.1:" + (startPort + i));
		}
		TimeUnit.SECONDS.timedJoin(runner, 50);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		STD_OUTPUT = STD_OUT_STREAM.stopRecording();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	
	@Test
	public void testFinishedInTime() {
		assertTrue("Scenario didn't finished in time", FINISHED_IN_TIME);
	}

	@Test
	public void testActualConcurrency() {
		assertEquals(STORAGE_DRIVERS_COUNT * EXPECTED_CONCURRENCY, ACTUAL_CONCURRENCY, 5);
	}
	
	@Test
	public void testMetricsStdout()
	throws Exception {
		final Map<IoType, Integer> concurrencyMap = new HashMap<>();
		concurrencyMap.put(IoType.CREATE, 20);
		concurrencyMap.put(IoType.READ, 50);
		testMetricsTableStdout(STD_OUTPUT, JOB_NAME, STORAGE_DRIVERS_COUNT, 0, concurrencyMap);
	}
}
