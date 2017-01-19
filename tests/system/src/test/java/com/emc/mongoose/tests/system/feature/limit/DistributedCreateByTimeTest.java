package com.emc.mongoose.tests.system.feature.limit;

import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 Created by andrey on 19.01.17.
 */
public class DistributedCreateByTimeTest
extends HttpStorageDistributedScenarioTestBase {

	private static final int LOAD_LIMIT_TIME = 25;

	private static boolean FINISHED_IN_TIME = true;
	private static String STD_OUTPUT = null;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		CONFIG_ARGS.add("--load-limit-time=" + LOAD_LIMIT_TIME);
		HttpStorageDistributedScenarioTestBase.setUpClass();
		final Thread runner = new Thread(
			() -> {
				try {
					STD_OUT_STREAM.startRecording();
					SCENARIO.run();
					STD_OUTPUT = STD_OUT_STREAM.stopRecording();
				} catch(final Throwable t) {
					LogUtil.exception(LOG, Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		try {
			TimeUnit.SECONDS.timedJoin(runner, LOAD_LIMIT_TIME + 2);
		} catch(final InterruptedException e) {
		}
		if(runner.isAlive()) {
			runner.interrupt();
			FINISHED_IN_TIME = false;
		}
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}

	@Test
	public void testFinishedInTime()
	throws Exception {
		assertTrue(FINISHED_IN_TIME);
	}

	@Test
	public void testSuccCountFromStdOut()
	throws Exception {

	}

	@Test
	public void testSuccCountFromLogFile()
	throws Exception {

	}

	@Test
	public void testSuccCountsEquals()
	throws Exception {

	}

	@Test
	public void testNoErrorsFromStdOut()
	throws Exception {

	}

	@Test
	public void testNoErrorsFromLogFile()
	throws Exception {

	}
}
