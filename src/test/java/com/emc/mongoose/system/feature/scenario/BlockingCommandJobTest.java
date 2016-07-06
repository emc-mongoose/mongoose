package com.emc.mongoose.system.feature.scenario;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
import com.emc.mongoose.system.base.LoggingTestBase;
import com.emc.mongoose.system.tools.BufferingOutputStream;
import com.emc.mongoose.system.tools.StdOutUtil;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 09.06.16.
 */
public class BlockingCommandJobTest
extends LoggingTestBase {

	private final static String RUN_ID = BlockingCommandJobTest.class.getCanonicalName();
	private final static long EXPECTED_DURATION_SEC = 10;
	private final static String MSG = "yohoho";
	private final static String CMD = "sleep " + EXPECTED_DURATION_SEC + "; echo "+ MSG;
	private final static String SCENARIO_JSON =
		"{" +
		"	\"type\" : \"command\",\n" +
		"	\"value\" : \"" + CMD + "\"\n" +
		"}\n";

	private static String STD_OUTPUT;
	private static long ACTUAL_DURATION_SEC;

	@BeforeClass
	public static void setUpClass() {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		try {
			LoggingTestBase.setUpClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
		try(
			final Scenario scenario = new JsonScenario(
				BasicConfig.THREAD_CONTEXT.get(), SCENARIO_JSON
			)
		) {
			try(final BufferingOutputStream stdOutStream = StdOutUtil.getStdOutBufferingStream()) {
				ACTUAL_DURATION_SEC = System.currentTimeMillis();
				scenario.run();
				ACTUAL_DURATION_SEC = TimeUnit.MILLISECONDS.toSeconds(
					System.currentTimeMillis() - ACTUAL_DURATION_SEC
				);
				TimeUnit.SECONDS.sleep(1);
				STD_OUTPUT = stdOutStream.toString();
			}
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
	}

	@Test
	public void checkJobDuration()
	throws Exception {
		Assert.assertEquals(EXPECTED_DURATION_SEC, ACTUAL_DURATION_SEC, 0.1);
	}

	@Test
	public void checkConsoleOutput() {
		Assert.assertTrue(STD_OUTPUT.contains("\u001B[36m" + CMD));
		Assert.assertTrue(STD_OUTPUT.contains("\u001B[34m" + MSG));
	}
}
