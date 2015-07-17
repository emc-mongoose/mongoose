package com.emc.mongoose.integ.distributed.chain;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
//
import com.emc.mongoose.util.scenario.Chain;
import com.emc.mongoose.util.scenario.shared.WSLoadBuilderFactory;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.concurrent.TimeUnit;
//
/**
 Created by kurila on 17.07.15.
 */
public class SimultaneousLoadTest {
	//
	private final static String LOAD_SEQ = "create,read,update,delete";
	//
	private static Logger LOG;
	private static long LOAD_LIMIT_TIME_SEC = 100, DURATION_TOTAL_SEC = -1;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LOG = LogManager.getLogger();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT, true);
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD, LOAD_SEQ);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, Long.toString(LOAD_LIMIT_TIME_SEC) + "s");
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		try(final LoadBuilder loadBuilder = WSLoadBuilderFactory.getInstance(rtConfig)) {
			final long timeOut = rtConfig.getLoadLimitTimeValue();
			final TimeUnit timeUnit = rtConfig.getLoadLimitTimeUnit();
			//
			final String[] loadTypeSeq = rtConfig.getScenarioChainLoad();
			final boolean isConcurrent = rtConfig.getScenarioChainConcurrentFlag();
			//
			final Chain chainScenario = new Chain(
				loadBuilder, timeOut, timeUnit, loadTypeSeq, isConcurrent
			);
			DURATION_TOTAL_SEC = System.currentTimeMillis() / 1000;
			chainScenario.run();
			DURATION_TOTAL_SEC = System.currentTimeMillis() / 1000 - DURATION_TOTAL_SEC;
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
	}
	//
	@Test
	public void checkTotalDuration()
	throws Exception {
		final int countSteps = LOAD_SEQ.split(",").length;
		Assert.assertTrue(
			"Actual duration (" + DURATION_TOTAL_SEC + "[s]) is less than expected (" +
			LOAD_LIMIT_TIME_SEC + "[s])",
			DURATION_TOTAL_SEC >= LOAD_LIMIT_TIME_SEC
		);
		Assert.assertTrue(
			"Actual duration (" + DURATION_TOTAL_SEC + "[s]) is too much more than expected (" +
			countSteps * LOAD_LIMIT_TIME_SEC + "[s])",
			DURATION_TOTAL_SEC - 10 < LOAD_LIMIT_TIME_SEC
		);
	}
}
