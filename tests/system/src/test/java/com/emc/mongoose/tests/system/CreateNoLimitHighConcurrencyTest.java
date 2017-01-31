package com.emc.mongoose.tests.system;

import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.tests.system.util.PortListener;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;

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
 * 2.2.3. New Items Input
 * 4.4. High Concurrency Level (1K)
 * 6.2.1. Load Jobs Are Infinite by Default
 * 10.2. Default Scenario
 * 10.5.2. Load Job
 * 11.1.2. Two Local Separate Storage Driver Services (at different ports)
 */
public class CreateNoLimitHighConcurrencyTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final int LOAD_CONCURRENCY = 1000;
	
	private static Thread RUNNER;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_JOB_NAME, CreateByTimeTest.class.getSimpleName());
		CONFIG_ARGS.add("--load-concurrency=" + LOAD_CONCURRENCY);
		HttpStorageDistributedScenarioTestBase.setUpClass();
		RUNNER = new Thread(
			() -> {
				try {
					SCENARIO.run();
				} catch(final Throwable t) {
					LogUtil.exception(LOG, Level.ERROR, t, "Failed to run the scenario");
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
		for(int i = 0; i < 10; i ++) {
			TimeUnit.SECONDS.sleep(10);
			activeConnCount = 0;
			for(int j = 0; j < STORAGE_NODE_COUNT; j ++) {
				activeConnCount += PortListener
					.getCountConnectionsOnPort("127.0.0.1:" + (startPort + j));
			}
			assertEquals(STORAGE_DRIVERS_COUNT * LOAD_CONCURRENCY, activeConnCount);
			System.out.println("Active connection count: " + activeConnCount);
		}
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		RUNNER.interrupt();
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
}
