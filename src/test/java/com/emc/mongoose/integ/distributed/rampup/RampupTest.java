package com.emc.mongoose.integ.distributed.rampup;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.conf.TimeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.base.DistributedLoadBuilderTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.storage.adapter.swift.Container;
import com.emc.mongoose.storage.adapter.swift.WSContainerImpl;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.util.scenario.Rampup;
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_SUM_CLIENT;
/**
 Created by andrey on 17.07.15.
 */
public class RampupTest
extends DistributedLoadBuilderTestBase {
	//
	private final static String
		LOAD_SEQ[] = {"create", "read", "delete"},
		SIZE_SEQ[] = {"1KB", "10KB", "100KB"},
		THREAD_COUNT_SEQ[] = {"1", "10", "100"};
	private final static int
		LOAD_LIMIT_TIME_SEC = 5,
		PRECISION_SEC = 10,
		COUNT_STEPS = LOAD_SEQ.length * SIZE_SEQ.length * THREAD_COUNT_SEQ.length;
	//
	private static long DURATION_TOTAL_SEQ = -1;
	private static byte STD_OUT_CONTENT[] = null;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RampupTest.class.getCanonicalName());
		System.setProperty(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT, "false");
		System.setProperty(RunTimeConfig.KEY_LOAD_LIMIT_TIME, Long.toString(LOAD_LIMIT_TIME_SEC) + "s");
		System.setProperty(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, "0");
		System.setProperty(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		System.setProperty(RunTimeConfig.KEY_API_NAME, "swift");
		DistributedLoadBuilderTestBase.setUpClass();
		final Rampup rampupScenario = new Rampup(
			LOAD_BUILDER_CLIENT, LOAD_LIMIT_TIME_SEC, TimeUnit.SECONDS,
			LOAD_SEQ, SIZE_SEQ, THREAD_COUNT_SEQ
		);
		//
		try(
			final BufferingOutputStream
				stdOutBuffer = StdOutInterceptorTestSuite.getStdOutBufferingStream()
		) {
			DURATION_TOTAL_SEQ = System.currentTimeMillis() / 1000;
			rampupScenario.run();
			DURATION_TOTAL_SEQ = System.currentTimeMillis() / 1000 - DURATION_TOTAL_SEQ;
			STD_OUT_CONTENT = stdOutBuffer.toByteArray();
		}
		//
		for(final RunIdFileManager manager: RunIdFileManager.LIST_MANAGERS) {
			for (final OutputStream out: manager.getOutStreamsMap().values()){
				out.flush();
			}
		}
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
					Assert.assertEquals("LatencyAvg[us]", nextRec.get(9));
					Assert.assertEquals("LatencyMin[us]", nextRec.get(10));
					Assert.assertEquals("LatencyMed[us]", nextRec.get(11));
					Assert.assertEquals("LatencyMax[us]", nextRec.get(12));
					Assert.assertEquals("TPAvg", nextRec.get(13));
					Assert.assertEquals("TP1Min", nextRec.get(14));
					Assert.assertEquals("TP5Min", nextRec.get(15));
					Assert.assertEquals("TP15Min", nextRec.get(16));
					Assert.assertEquals("BWAvg[MB/s]", nextRec.get(17));
					Assert.assertEquals("BW1Min[MB/s]", nextRec.get(18));
					Assert.assertEquals("BW5Min[MB/s]", nextRec.get(19));
					Assert.assertEquals("BW15Min[MB/s]", nextRec.get(20));
				} else {
					if(nextRec.size() == 21) {
						final String countSrvStr = nextRec.get(6);
						if(countSrvStr.length() > 0 && Integer.parseInt(countSrvStr) == 1) {
							countSummaries++;
						}
					}
				}
			}
		}
		Assert.assertEquals("Wrong summary log statements count", COUNT_STEPS, countSummaries);
	}
}
