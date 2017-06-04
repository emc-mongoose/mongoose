package com.emc.mongoose.tests.system.deprecated;

import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.tests.system.util.PortListener;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 31.01.17.
 Covered use cases:
 * 2.1.1.1.3. Default Data Items (1MB)
 * 4.4. High Concurrency Level (1K)
 * 6.2.1. Load Jobs Are Infinite by Default
 * 9.2. Default Scenario
 * 9.5.2. Load Job
 * 10.4.4. Two Local Separate Storage Driver Services (at different ports)
 */
public class CreateNoLimitHighConcurrencyTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final int LOAD_CONCURRENCY = 1000;
	
	private static Thread RUNNER;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = CreateNoLimitHighConcurrencyTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		CONFIG_ARGS.add("--storage-driver-concurrency=" + LOAD_CONCURRENCY);
		HttpStorageDistributedScenarioTestBase.setUpClass();
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
	}
	
	@Test
	public final void testActiveConnectionsCount()
	throws Exception {
		final int startPort = CONFIG.getStorageConfig().getNetConfig().getNodeConfig().getPort();
		int activeConnCount;
		TimeUnit.SECONDS.sleep(30);
		activeConnCount = 0;
		for(int j = 0; j < STORAGE_NODE_COUNT; j ++) {
			activeConnCount += PortListener
				.getCountConnectionsOnPort("127.0.0.1:" + (startPort + j));
		}
		assertEquals(
			STORAGE_DRIVERS_COUNT * LOAD_CONCURRENCY, activeConnCount,
			STORAGE_DRIVERS_COUNT * LOAD_CONCURRENCY / 1000
		);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		RUNNER.interrupt();
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
}
