package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
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
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 04.06.17.
 */
public class CreateNoLimitTest
extends EnvConfiguredScenarioTestBase {

	private static Thread RUNNER;
	private static String ITEM_OUTPUT_PATH;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("s3", "swift"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(100));
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		STEP_NAME = CreateNoLimitTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, STEP_NAME);
		EnvConfiguredScenarioTestBase.setUpClass();
		
		switch(STORAGE_DRIVER_TYPE) {
			case STORAGE_TYPE_FS:
				ITEM_OUTPUT_PATH = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
				).toString();
				CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
				break;
			case STORAGE_TYPE_SWIFT:
				CONFIG.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
				break;
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());

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
		if(STORAGE_TYPE_FS.equals(STORAGE_DRIVER_TYPE)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(ITEM_OUTPUT_PATH);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Test
	public final void testActualConcurrencyCount()
	throws Exception {
		final int expectedConcurrency = STORAGE_DRIVERS_COUNT * CONCURRENCY;
		if(STORAGE_TYPE_FS.equals(STORAGE_DRIVER_TYPE)) {
			final int actualConcurrency = OpenFilesCounter.getOpenFilesCount(ITEM_OUTPUT_PATH);
			assertTrue(
				"Expected concurrency <= " + actualConcurrency + ", actual: " + actualConcurrency,
				actualConcurrency <= expectedConcurrency
			);
		} else {
			int actualConcurrency = 0;
			final int startPort = CONFIG.getStorageConfig().getNetConfig().getNodeConfig().getPort();
			for(int j = 0; j < HTTP_STORAGE_NODE_COUNT; j ++) {
				actualConcurrency += PortListener
					.getCountConnectionsOnPort("127.0.0.1:" + (startPort + j));
			}
			assertEquals(
				"Expected concurrency: " + actualConcurrency + ", actual: " + actualConcurrency,
				expectedConcurrency, actualConcurrency, expectedConcurrency / 100
			);
		}

	}
}
