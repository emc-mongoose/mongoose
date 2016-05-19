package com.emc.mongoose.system.base;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.run.scenario.runner.ScenarioRunner;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 19.05.16.
 */
public abstract class ScenarioTestBase
extends WSMockTestBase {

	protected static ScenarioRunner SCENARIO_RUNNER;

	@BeforeClass
	public static void setUpClass() {
		try {
			WSMockTestBase.setUpClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to set up the base test");
		}
		SCENARIO_RUNNER = new ScenarioRunner(BasicConfig.THREAD_CONTEXT.get());
		SCENARIO_RUNNER.run();
	}

	@AfterClass
	public static void tearDownClass() {
		try {
			WSMockTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to tear down the base test");
		}
		SCENARIO_RUNNER = null;
	}
}
