package com.emc.mongoose.tests.system;

import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.OpenFilesCounter;
import com.emc.mongoose.tests.system.util.PortListener;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 04.06.17.
 */
public class CreateNoLimitTest
extends EnvConfiguredScenarioTestBase {

	private static Thread RUNNER;
	private static String ITEM_OUTPUT_PATH;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = CreateNoLimitTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(STORAGE_TYPE_FS_KEY.equals(STORAGE_DRIVER_TYPE)) {
			ITEM_OUTPUT_PATH = Files.createTempDirectory(JOB_NAME).toString();
			// need to re-init the scenario
			SCENARIO.close();
			CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
			SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		}
		RUNNER = new Thread(
			() -> {
				try {
					SCENARIO.run();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		RUNNER.start();
		TimeUnit.SECONDS.sleep(25);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(RUNNER != null) {
			RUNNER.interrupt();
		}
		if(STORAGE_TYPE_FS_KEY.equals(STORAGE_DRIVER_TYPE)) {
			Files.delete(Paths.get(ITEM_OUTPUT_PATH));
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Test
	public final void testActualConcurrencyCount()
	throws Exception {
		int actualConcurrency = 0;
		if(STORAGE_TYPE_FS_KEY.equals(STORAGE_DRIVER_TYPE)) {
			actualConcurrency = OpenFilesCounter.getOpenFilesCount(ITEM_OUTPUT_PATH);
		} else {
			final int startPort = CONFIG.getStorageConfig().getNetConfig().getNodeConfig().getPort();
			for(int j = 0; j < HTTP_STORAGE_NODE_COUNT; j ++) {
				actualConcurrency += PortListener
					.getCountConnectionsOnPort("127.0.0.1:" + (startPort + j));
			}
		}
		assertEquals(
			STORAGE_DRIVERS_COUNT * CONCURRENCY, actualConcurrency,
			STORAGE_DRIVERS_COUNT * CONCURRENCY / 10
		);
	}
}
