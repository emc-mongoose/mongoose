package com.emc.mongoose.integ.distributed.single;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import static com.emc.mongoose.integ.tools.LogPatterns.*;

import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicStorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
//
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
/**
 Created by kurila on 16.07.15.
 */
public class WriteLoggingTest {
	//
	private final static long COUNT_TO_WRITE = 1000;
	private final static String RUN_ID = WriteLoggingTest.class.getCanonicalName();
	//
	private static long COUNT_WRITTEN;
	private static Logger LOG;
	private static byte STD_OUT_CONTENT[];
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		TimeUnit.SECONDS.sleep(5);
		// reinit run id and the log path
		RunTimeConfig
			.getContext()
			.set(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		LoggingTestSuite.setUpClass();
		//
		try(
			final StorageClient<WSObject>
				client = new BasicWSClientBuilder<>()
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(COUNT_TO_WRITE)
					.setClientMode(new String[] {ServiceUtils.getHostAddr()})
					.build()
		) {
			try(
				final BufferingOutputStream
					stdOutInterceptorStream = StdOutInterceptorTestSuite.getStdOutBufferingStream()
			) {
				COUNT_WRITTEN = client.write(null, null, (short) 10, SizeUtil.toSize("10KB"));
				TimeUnit.SECONDS.sleep(1);
				STD_OUT_CONTENT = stdOutInterceptorStream.toByteArray();
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
	public void checkConsoleAvgMetricsLogging()
	throws Exception {
		boolean passed = false;
		long lastSuccCount = 0;
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
					m = CONSOLE_METRICS_AVG_CLIENT.matcher(nextStdOutLine);
					if(m.find()) {
						Assert.assertTrue(
							"Load type is not " + IOTask.Type.CREATE.name(),
							IOTask.Type.CREATE.name().toLowerCase().equals(
								m.group("typeLoad").toLowerCase()
							)
						);
						long
							nextSuccCount = Long.parseLong(m.group("countSucc")),
							nextFailCount = Long.parseLong(m.group("countFail"));
						Assert.assertTrue(
							nextStdOutLine +
							": next written items count " + nextSuccCount +
							" is less than previous: " + lastSuccCount,
							nextSuccCount >= lastSuccCount
						);
						lastSuccCount = nextSuccCount;
						Assert.assertTrue("There are failures reported", nextFailCount == 0);
						passed = true;
					}
				}
			} while(true);
		}
		Assert.assertTrue(
			"Average metrics line matching the pattern was not met in the stdout", passed
		);
	}
	//
	@Test
	public void checkConsoleSumMetricsLogging()
	throws Exception {
		boolean passed = false;
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
						Assert.assertTrue(
							"Load type is not " + IOTask.Type.CREATE.name(),
							IOTask.Type.CREATE.name().toLowerCase().equals(
								m.group("typeLoad").toLowerCase()
							)
						);
						long
							countLimit = Long.parseLong(m.group("countLimit")),
							countSucc = Long.parseLong(m.group("countSucc")),
							countFail = Long.parseLong(m.group("countFail"));
						Assert.assertTrue(
							"Written items count " + countSucc +
								" is not equal to the limit: " + countLimit,
							countSucc == countLimit
						);
						Assert.assertTrue("There are failures reported", countFail == 0);
						Assert.assertFalse("Summary metrics are printed twice at least", passed);
						passed = true;
					}
				}
			} while(true);
		}
		Assert.assertTrue(
			"Summary metrics line matching the pattern was not met in the stdout", passed
		);
	}
	//
	@Test
	public void checkFileAvgMetricsLogging()
	throws Exception {
		boolean firstRow = true, secondRow = false;
		final File logPerfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("Performance avg metrics log file doesn't exist", logPerfAvgFile.exists());
		try(
			final BufferedReader
				in = Files.newBufferedReader(logPerfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					Assert.assertEquals("DateTimeISO8601", nextRec.get(0));
					Assert.assertEquals("LoadId", nextRec.get(1));
					Assert.assertEquals("TypeAPI", nextRec.get(2));
					Assert.assertEquals("TypeLoad", nextRec.get(3));
					Assert.assertEquals("CountConn", nextRec.get(4));
					Assert.assertEquals("CountNode", nextRec.get(5));
					Assert.assertEquals("CountLoadServer", nextRec.get(6));
					Assert.assertEquals("CountSucc", nextRec.get(7));
					Assert.assertEquals("CountPending", nextRec.get(8));
					Assert.assertEquals("CountFail", nextRec.get(9));
					Assert.assertEquals("LatencyAvg[us]", nextRec.get(10));
					Assert.assertEquals("LatencyMin[us]", nextRec.get(11));
					Assert.assertEquals("LatencyMed[us]", nextRec.get(12));
					Assert.assertEquals("LatencyMax[us]", nextRec.get(13));
					Assert.assertEquals("TPAvg", nextRec.get(14));
					Assert.assertEquals("TP1Min", nextRec.get(15));
					Assert.assertEquals("TP5Min", nextRec.get(16));
					Assert.assertEquals("TP15Min", nextRec.get(17));
					Assert.assertEquals("BWAvg[MB/s]", nextRec.get(18));
					Assert.assertEquals("BW1Min[MB/s]", nextRec.get(19));
					Assert.assertEquals("BW5Min[MB/s]", nextRec.get(20));
					Assert.assertEquals("BW15Min[MB/s]", nextRec.get(21));
					firstRow = false;
				} else {
					secondRow = true;
					Assert.assertTrue(
						"Load type is \"" + nextRec.get(3) + "\" but \"Create\" is expected",
						IOTask.Type.CREATE.name().equalsIgnoreCase(nextRec.get(3))
					);
				}
			}
		}
		Assert.assertTrue("Average metrics record was not found in the log file", secondRow);
	}
	//
	@Test
	public void checkFileSumMetricsLogging()
	throws Exception {
		boolean firstRow = true, secondRow = false;
		final File logPerfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue("Performance sum metrics log file doesn't exist", logPerfSumFile.exists());
		try(
			final BufferedReader
				in = Files.newBufferedReader(logPerfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
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
					firstRow = false;
				} else {
					secondRow = true;
					Assert.assertTrue(
						"Load type is \"" + nextRec.get(3) + "\" but \"Create\" is expected",
						IOTask.Type.CREATE.name().equalsIgnoreCase(nextRec.get(3))
					);
				}
			}
		}
		Assert.assertTrue("Summary metrics record was not found in the log file", secondRow);
	}
}
