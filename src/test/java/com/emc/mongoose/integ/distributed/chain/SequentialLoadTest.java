package com.emc.mongoose.integ.distributed.chain;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.base.DistributedLoadBuilderTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.storage.adapter.s3.Bucket;
import com.emc.mongoose.storage.adapter.s3.WSBucketImpl;
import com.emc.mongoose.storage.adapter.s3.WSRequestConfigImpl;
import com.emc.mongoose.util.scenario.Chain;
//
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
//
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
	private long durationTotalSec = -1;
	private byte stdOutContent[] = null;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_API_NAME, "s3");
		DistributedLoadBuilderTestBase.setUpClass();
	}
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		final Chain chainScenario = new Chain(
			loadBuilderClient, LOAD_JOB_TIME_LIMIT_SEC, TimeUnit.SECONDS, LOAD_SEQ, false, true
		);
		try(
			final BufferingOutputStream
				stdOutBuffer = StdOutInterceptorTestSuite.getStdOutBufferingStream()
		) {
			durationTotalSec = System.currentTimeMillis() / 1000;
			chainScenario.run();
			durationTotalSec = System.currentTimeMillis() / 1000 - durationTotalSec;
			stdOutContent = stdOutBuffer.toByteArray();
		}
	}
	//
	@After
	public void tearDown()
	throws Exception {
		final Bucket bucket = new WSBucketImpl(
			(WSRequestConfigImpl) WSRequestConfigBase.newInstanceFor("s3").setProperties(RT_CONFIG),
			RT_CONFIG.getString(RunTimeConfig.KEY_API_S3_BUCKET), false
		);
		bucket.delete(RT_CONFIG.getStorageAddrs()[0]);
		super.tearDown();
	}
	//
	@Test
	public void checkTotalDuration()
	throws Exception {
		Assert.assertTrue(
			"Actual duration (" + durationTotalSec + "[s]) is much more than expected (" +
			COUNT_STEPS * LOAD_JOB_TIME_LIMIT_SEC + "[s])",
			durationTotalSec <= PRECISION_SEC + COUNT_STEPS * LOAD_JOB_TIME_LIMIT_SEC
		);
	}
	//
	@Test
	public void checkLogStdOutSummariesCount()
		throws Exception {
		int countSummaries = 0;
		try(
			final BufferedReader in = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(stdOutContent))
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
		Assert.assertTrue("Performance sum metrics file doesn't exist", fileLogPerfSum.exists());
		try(
			final BufferedReader
				in = Files.newBufferedReader(fileLogPerfSum.toPath(), StandardCharsets.UTF_8)
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
