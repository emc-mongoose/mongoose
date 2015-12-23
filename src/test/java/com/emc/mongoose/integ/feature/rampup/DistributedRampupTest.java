package com.emc.mongoose.integ.feature.rampup;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.integ.base.DistributedLoadBuilderTestBase;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.run.scenario.Rampup;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_SUM_CLIENT;
/**
 Created by andrey on 17.07.15.
 */
public class DistributedRampupTest
extends DistributedLoadBuilderTestBase {
	//
	private final static String
		LOAD_SEQ[] = {"create", "read", "delete"},
		SIZE_SEQ[] = {"1KB", "10KB", "100KB"},
		THREAD_COUNT_SEQ[] = {"1", "10", "100"};
	private final static int
		LOAD_LIMIT_TIME_SEC = 20,
		PRECISION_SEC = 10,
		COUNT_STEPS = LOAD_SEQ.length * SIZE_SEQ.length * THREAD_COUNT_SEQ.length;
	//
	private static long DURATION_TOTAL_SEQ = -1;
	private static byte STD_OUT_CONTENT[] = null;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, DistributedRampupTest.class.getCanonicalName());
		DistributedLoadBuilderTestBase.setUpClass();
		//
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT, "false");
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, Long.toString(LOAD_LIMIT_TIME_SEC) + "s");
		rtConfig.set(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, "0");
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		rtConfig.set(RunTimeConfig.KEY_API_NAME, "swift");
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD, LOAD_SEQ);
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_RAMPUP_SIZES, SIZE_SEQ);
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_RAMPUP_CONN_COUNTS, THREAD_COUNT_SEQ);
		RunTimeConfig.setContext(rtConfig);
		//
		final Rampup rampupScenario = new Rampup(rtConfig);
		//
		try(
			final BufferingOutputStream
				stdOutBuffer = StdOutUtil.getStdOutBufferingStream()
		) {
			DURATION_TOTAL_SEQ = System.currentTimeMillis() / 1000;
			rampupScenario.run();
			DURATION_TOTAL_SEQ = System.currentTimeMillis() / 1000 - DURATION_TOTAL_SEQ;
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
			"Actual duration (" + DURATION_TOTAL_SEQ + "[s]) is much more than expected (" +
			COUNT_STEPS * LOAD_LIMIT_TIME_SEC + "[s])",
			DURATION_TOTAL_SEQ <= PRECISION_SEC + COUNT_STEPS * LOAD_LIMIT_TIME_SEC
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
			"Wrong summary log statements count in the stdout", COUNT_STEPS, countSummaries
		);
	}
	//
	@Test public void checkLogFileSummariesCount()
	throws Exception {
		int countSummaries = 0;
		Assert.assertTrue("Performance sum metrics file doesn't exist", FILE_LOG_PERF_SUM.exists());
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_PERF_SUM.toPath(), StandardCharsets.UTF_8)
		) {
			final List<CSVRecord> csvRecs = CSVFormat.RFC4180.parse(in).getRecords();
			CSVRecord nextRec;
			if(csvRecs.size() > 0) {
				nextRec = csvRecs.get(0);
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
			}
			for(int i = 1; i < csvRecs.size(); i ++) {
				nextRec = csvRecs.get(i);
				if(nextRec.size() == 25) {
					final String countSrvStr = nextRec.get(6);
					if(countSrvStr.length() > 0 && Integer.parseInt(countSrvStr) == 1) {
						countSummaries ++;
					}
				} else {
					LOG.info(Markers.MSG, "Req #{} is not a summary statement: {}", i, nextRec);
				}
			}
		}
		Assert.assertEquals("Wrong summary log statements count", COUNT_STEPS, countSummaries);
	}
}
