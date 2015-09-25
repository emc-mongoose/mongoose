package com.emc.mongoose.integ.distributed.chain;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.integ.base.DistributedLoadBuilderTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.run.scenario.Chain;
//
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
//
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_SUM_CLIENT;
/**
 Created by kurila on 17.07.15.
 */
public class SequentialLoadTest
extends DistributedLoadBuilderTestBase {
	//
	private final static String
		RUN_ID = SequentialLoadTest.class.getCanonicalName(),
		LOAD_SEQ[] = { "create", "read", "read", "delete", "delete" };
	private static final int
		LOAD_JOB_TIME_LIMIT_SEC = 30,
		PRECISION_SEC = 10,
		COUNT_STEPS = LOAD_SEQ.length;
	//
	private static long DURATION_TOTAL_SEC = -1;
	private static byte STD_OUT_CONTENT[] = null;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_API_NAME, "s3");
		DistributedLoadBuilderTestBase.setUpClass();
		final Chain chainScenario = new Chain(
			LOAD_BUILDER_CLIENT, LOAD_JOB_TIME_LIMIT_SEC, TimeUnit.SECONDS, LOAD_SEQ, false, true
		);
		try(
			final BufferingOutputStream
				stdOutBuffer = StdOutInterceptorTestSuite.getStdOutBufferingStream()
		) {
			DURATION_TOTAL_SEC = System.currentTimeMillis() / 1000;
			chainScenario.run();
			DURATION_TOTAL_SEC = System.currentTimeMillis() / 1000 - DURATION_TOTAL_SEC;
			STD_OUT_CONTENT = stdOutBuffer.toByteArray();
		}
		//
		RunIdFileManager.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}
	//
	@Test public void checkTotalDuration()
	throws Exception {
		Assert.assertTrue(
			"Actual duration (" + DURATION_TOTAL_SEC + "[s]) is much more than expected (" +
			COUNT_STEPS * LOAD_JOB_TIME_LIMIT_SEC + "[s])",
			DURATION_TOTAL_SEC <= PRECISION_SEC + COUNT_STEPS * LOAD_JOB_TIME_LIMIT_SEC
		);
	}
	//
	@Test public void checkLogStdOutSummariesCount()
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
			"Invalid summary log statements in the std out", COUNT_STEPS, countSummaries
		);
	}
	//
	@Test public void checkLogFileSummariesCount()
	throws Exception {
		boolean firstRow = true;
		int countSummaries = 0;
		Assert.assertTrue("Performance sum metrics file doesn't exist", FILE_LOG_PERF_SUM.exists());
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_PERF_SUM.toPath(), StandardCharsets.UTF_8)
		) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
					Assert.assertEquals("DateTimeISO8601", nextRec.get(0));
					Assert.assertEquals("LoadId", nextRec.get(1));
					Assert.assertEquals("TypeAPI", nextRec.get(2));
					Assert.assertEquals("TypeLoad", nextRec.get(3));
					Assert.assertEquals("CountConn", nextRec.get(4));
					Assert.assertEquals("CountNode", nextRec.get(5));
					Assert.assertEquals("CountLoadServer", nextRec.get(6));
					Assert.assertEquals("CountSucc", nextRec.get(7));
					Assert.assertEquals("CountFail", nextRec.get(8));
					Assert.assertEquals("DurationAvg[us]", nextRec.get(9));
					Assert.assertEquals("DurationMin[us]", nextRec.get(10));
					Assert.assertEquals("DurationStdDev", nextRec.get(11));
					Assert.assertEquals("DurationMax[us]", nextRec.get(12));
					Assert.assertEquals("LatencyAvg[us]", nextRec.get(13));
					Assert.assertEquals("LatencyMin[us]", nextRec.get(14));
					Assert.assertEquals("LatencyStdDev", nextRec.get(15));
					Assert.assertEquals("LatencyMax[us]", nextRec.get(16));
					Assert.assertEquals("TPAvg[s^-1]", nextRec.get(17));
					Assert.assertEquals("TPLast[s^-1]", nextRec.get(18));
					Assert.assertEquals("BWAvg[MB*s^-1]", nextRec.get(19));
					Assert.assertEquals("BWLast[MB*s^-1]", nextRec.get(20));

				} else {
					final String countSrvStr = nextRec.get(6);
					if(countSrvStr.length() > 0 && Integer.parseInt(countSrvStr) == 1) {
						countSummaries ++;
					}
				}
			}
		}
		Assert.assertEquals("Wrong summary log statements count", COUNT_STEPS, countSummaries);
	}
}
