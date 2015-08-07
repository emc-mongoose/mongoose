package com.emc.mongoose.integ.distributed.chain;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
//
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.storage.adapter.atmos.SubTenant;
import com.emc.mongoose.storage.adapter.atmos.WSRequestConfigImpl;
import com.emc.mongoose.storage.adapter.atmos.WSSubTenantImpl;
import com.emc.mongoose.util.scenario.Chain;
import com.emc.mongoose.util.scenario.shared.WSLoadBuilderFactory;
//
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_SUM_CLIENT;
//
/**
 Created by kurila on 17.07.15.
 */
public class SimultaneousLoadTest {
	//
	private final static String
		RUN_ID = SimultaneousLoadTest.class.getCanonicalName(),
		LOAD_SEQ[] = { "create", "read", "update", "append", "delete" };
	private final static int
		LOAD_LIMIT_TIME_SEC = 100,
		PRECISION_SEC = 10,
		COUNT_STEPS = LOAD_SEQ.length;
	//
	private static long DURATION_TOTAL_SEC = -1;
	private static byte STD_OUT_CONTENT[] = null;
	private static File FILE_LOG_PERF_SUM;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, 0);
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		rtConfig.set(RunTimeConfig.KEY_API_NAME, "atmos");
		FILE_LOG_PERF_SUM = LogParser.getPerfSumFile(RUN_ID);
		if(FILE_LOG_PERF_SUM.exists()) {
			FILE_LOG_PERF_SUM.delete();
		}
		try(final LoadBuilder loadBuilder = WSLoadBuilderFactory.getInstance(rtConfig)) {
			final Chain chainScenario = new Chain(
				loadBuilder, LOAD_LIMIT_TIME_SEC, TimeUnit.SECONDS, LOAD_SEQ, true
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
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		final SubTenant st = new WSSubTenantImpl(
			(WSRequestConfigImpl) WSLoadBuilderFactory.getInstance(rtConfig).getRequestConfig(),
			rtConfig.getString(RunTimeConfig.KEY_API_ATMOS_SUBTENANT)
		);
		st.delete(rtConfig.getStorageAddrs()[0]);
	}
	//
	@Test
	public void checkTotalDuration()
	throws Exception {
		Assert.assertEquals(
			"Actual duration is not equal to the expected",
			LOAD_LIMIT_TIME_SEC, DURATION_TOTAL_SEC, PRECISION_SEC
		);
	}
	//
	@Test
	public void checkLogStdOutSummariesCount()
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
	@Test
	public void checkLogFileSummariesCount()
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
