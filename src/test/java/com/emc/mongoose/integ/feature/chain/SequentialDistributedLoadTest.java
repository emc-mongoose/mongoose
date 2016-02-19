package com.emc.mongoose.integ.feature.chain;
//
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.integ.base.DistributedLoadBuilderTestBase;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
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
public class SequentialDistributedLoadTest
extends DistributedLoadBuilderTestBase {
	//
	private final static String
		RUN_ID = SequentialDistributedLoadTest.class.getCanonicalName(),
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
		DistributedLoadBuilderTestBase.setUpClass();
		//
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.set(RunTimeConfig.KEY_API_NAME, "s3");
		appConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD, LOAD_SEQ);
		appConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LOAD_JOB_TIME_LIMIT_SEC + "s");
		appConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT, false);
		RunTimeConfig.setContext(appConfig);
		//
		final Chain chainScenario = new Chain(appConfig);
		try(
			final BufferingOutputStream
				stdOutBuffer = StdOutUtil.getStdOutBufferingStream()
		) {
			DURATION_TOTAL_SEC = System.currentTimeMillis() / 1000;
			chainScenario.run();
			DURATION_TOTAL_SEC = System.currentTimeMillis() / 1000 - DURATION_TOTAL_SEC;
			TimeUnit.SECONDS.sleep(10);
			STD_OUT_CONTENT = stdOutBuffer.toByteArray();
		}
		//
		RunIdFileManager.flushAll();
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
		final StringBuilder strb = new StringBuilder();
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
					Assert.assertEquals("DurationLoQ[us]", nextRec.get(11));
					Assert.assertEquals("DurationMed[us]", nextRec.get(12));
					Assert.assertEquals("DurationHiQ[us]", nextRec.get(13));
					Assert.assertEquals("DurationMax[us]", nextRec.get(14));
					Assert.assertEquals("LatencyAvg[us]", nextRec.get(15));
					Assert.assertEquals("LatencyMin[us]", nextRec.get(16));
					Assert.assertEquals("LatencyLoQ[us]", nextRec.get(17));
					Assert.assertEquals("LatencyMed[us]", nextRec.get(18));
					Assert.assertEquals("LatencyHiQ[us]", nextRec.get(19));
					Assert.assertEquals("LatencyMax[us]", nextRec.get(20));
					Assert.assertEquals("TPAvg[op/s]", nextRec.get(21));
					Assert.assertEquals("TPLast[op/s]", nextRec.get(22));
					Assert.assertEquals("BWAvg[MB/s]", nextRec.get(23));
					Assert.assertEquals("BWLast[MB/s]", nextRec.get(24));
				} else {
					final String countSrvStr = nextRec.get(6);
					if(countSrvStr.length() > 0 && Integer.parseInt(countSrvStr) == 1) {
						strb.append('\n').append(nextRec.toString());
						countSummaries ++;
					}
				}
			}
		}
		Assert.assertEquals(
			"Wrong summary log statements count:" + strb.toString(), COUNT_STEPS, countSummaries
		);
	}
}
