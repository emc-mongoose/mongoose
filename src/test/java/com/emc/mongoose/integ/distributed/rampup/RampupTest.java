package com.emc.mongoose.integ.distributed.rampup;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
//
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.util.scenario.Rampup;
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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_SUM_CLIENT;
/**
 Created by andrey on 17.07.15.
 */
public class RampupTest {
	//
	private final static String
		RUN_ID = RampupTest.class.getCanonicalName(),
		LOAD_SEQ[] = {"create", "read", "delete"},
		SIZE_SEQ[] = {"1KB", "10KB", "100KB"},
		THREAD_COUNT_SEQ[] = {"1", "10", "100"};
	private final static int
		LOAD_LIMIT_TIME_SEC = 50,
		PRECISION_SEC = 10,
		COUNT_STEPS = LOAD_SEQ.length * SIZE_SEQ.length * THREAD_COUNT_SEQ.length;
	//
	private static Logger LOG;
	private static long DURATION_TOTAL_SEC = -1;
	private static byte STD_OUT_CONTENT[] = null;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LOG = LogManager.getLogger();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT, false);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, Long.toString(LOAD_LIMIT_TIME_SEC) + "s");
		rtConfig.set(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, 0);
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		try(final LoadBuilder loadBuilder = WSLoadBuilderFactory.getInstance(rtConfig)) {
			final Rampup rampupScenario = new Rampup(
				loadBuilder, LOAD_LIMIT_TIME_SEC, TimeUnit.SECONDS,
				LOAD_SEQ, SIZE_SEQ, THREAD_COUNT_SEQ
			);
			//
			try(
				final BufferingOutputStream
					stdOutBuffer = StdOutInterceptorTestSuite.getStdOutBufferingStream()
			) {
				DURATION_TOTAL_SEC = System.currentTimeMillis() / 1000;
				rampupScenario.run();
				DURATION_TOTAL_SEC = System.currentTimeMillis() / 1000 - DURATION_TOTAL_SEC;
				STD_OUT_CONTENT = stdOutBuffer.toByteArray();
			}
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
		Assert.assertTrue(
			"Actual duration (" + DURATION_TOTAL_SEC + "[s]) is much more than expected (" +
			COUNT_STEPS * LOAD_LIMIT_TIME_SEC + "[s])",
			DURATION_TOTAL_SEC <= PRECISION_SEC + COUNT_STEPS * LOAD_LIMIT_TIME_SEC
		);
	}
	//
	@Test
	public void checkLogFileSummariesCount()
	throws Exception {
		int countSummaries = 0;
		try(
			final BufferedReader in = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(STD_OUT_CONTENT))
			)
		) {
			String nextStdOutLine;
			Matcher m;
			do {
				nextStdOutLine = in.readLine();
				if(nextStdOutLine == null) {
					break;
				} else {
					m = CONSOLE_METRICS_SUM_CLIENT.matcher(nextStdOutLine);
					if(m.find()) {
						countSummaries ++;
					}
				}
			} while(true);
		}
		Assert.assertEquals(
			"Got " + countSummaries + " summary points while expected " + COUNT_STEPS,
			countSummaries, COUNT_STEPS
		);
	}
}
