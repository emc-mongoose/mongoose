package com.emc.mongoose.system.feature.distributed;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
//
import com.emc.mongoose.core.impl.item.base.LimitedQueueItemBuffer;
//
import com.emc.mongoose.system.base.DistributedClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
//
import com.emc.mongoose.system.tools.StdOutUtil;
import static com.emc.mongoose.system.tools.LogPatterns.*;

import com.emc.mongoose.system.tools.BufferingOutputStream;
//
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
/**
 Created by kurila on 16.07.15.
 */
public class UpdateLoggingTest
extends DistributedClientTestBase {
	//
	private final static int COUNT_LIMIT = 10000;
	//
	private static long countWritten, countUpdated;
	private static byte stdOutContent[];
	//
	@BeforeClass
	public static void setUpClass() {
		System.setProperty(AppConfig.KEY_RUN_ID, UpdateLoggingTest.class.getCanonicalName());
		try {
			DistributedClientTestBase.setUpClass();
			try(
				final StorageClient<HttpDataItem> client = CLIENT_BUILDER
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(COUNT_LIMIT)
					.setAPI("swift")
					.setNamespace("swift")
					.build()
			) {
				final BlockingQueue<HttpDataItem> itemsQueue = new ArrayBlockingQueue<>(COUNT_LIMIT);
				final LimitedQueueItemBuffer<HttpDataItem> itemsIO = new LimitedQueueItemBuffer<>(itemsQueue);
				countWritten = client.create(itemsIO, COUNT_LIMIT, 10, SizeInBytes.toFixedSize("10KB"));
				TimeUnit.SECONDS.sleep(10);
				Assert.assertEquals(
					"Writing reported different count than available in the output",
					countWritten, itemsQueue.size()
				);
				try(
					final BufferingOutputStream stdOutInterceptorStream = StdOutUtil.getStdOutBufferingStream()
				) {
					stdOutInterceptorStream.reset(); // clear before using
					if(countWritten > 0) {
						countUpdated = client.update(itemsIO, null, countWritten, 10, 10);
					}
					TimeUnit.SECONDS.sleep(1);
					stdOutContent = stdOutInterceptorStream.toByteArray();
				}
			}
			LOG.info(
				Markers.MSG, "Deleted {} items, captured {} bytes from stdout", countUpdated,
				stdOutContent.length
			);
			//
			RunIdFileManager.flushAll();
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		StdOutUtil.reset();
		DistributedClientTestBase.tearDownClass();
	}
	//
	@Test public void checkConsoleAvgMetricsLogging()
	throws Exception {
		boolean passed = false;
		long lastSuccCount = 0;
		try(
			final BufferedReader in = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(stdOutContent))
			)
		) {
			String nextStdOutLine, loadType;
			Matcher m;
			do {
				nextStdOutLine = in.readLine();
				if(nextStdOutLine == null) {
					break;
				} else {
					m = CONSOLE_METRICS_AVG_CLIENT.matcher(nextStdOutLine);
					if(m.find()) {
						loadType = m.group("typeLoad");
						Assert.assertTrue(
							"Invalid load type int the line:\n" + nextStdOutLine,
							LoadType.UPDATE.name().toLowerCase().equals(loadType.toLowerCase())
						);
						long
							nextSuccCount = Long.parseLong(m.group("countSucc")),
							nextFailCount = Long.parseLong(m.group("countFail"));
						Assert.assertTrue(
							"Next updated items count " + nextSuccCount +
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
	@Test public void checkConsoleSumMetricsLogging()
	throws Exception {
		boolean passed = false;
		try(
			final BufferedReader in = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(stdOutContent))
			)
		) {
			String nextStdOutLine, loadType;
			Matcher m;
			do {
				nextStdOutLine = in.readLine();
				if(nextStdOutLine == null) {
					break;
				} else {
					m = CONSOLE_METRICS_SUM_CLIENT.matcher(nextStdOutLine);
					if(m.find()) {
						loadType = m.group("typeLoad");
						Assert.assertTrue(
							"Invalid load type in the line: \n" + nextStdOutLine,
							LoadType.UPDATE.name().toLowerCase().equals(loadType.toLowerCase())
						);
						long
							countLimit = Long.parseLong(m.group("countLimit")),
							countSucc = Long.parseLong(m.group("countSucc")),
							countFail = Long.parseLong(m.group("countFail"));
						Assert.assertTrue(
							"Updated items count " + countSucc +
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
	@Test public void checkFileAvgMetricsLogging()
		throws Exception {
		boolean firstRow = true, secondRow = false;
		Assert.assertTrue(
			"Performance avg metrics log file \"" + FILE_LOG_PERF_AVG + "\" doesn't exist",
			FILE_LOG_PERF_AVG.exists()
		);
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_PERF_AVG.toPath(), StandardCharsets.UTF_8)
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
					Assert.assertEquals("DurationAvg[us]", nextRec.get(9));
					Assert.assertEquals("DurationMin[us]", nextRec.get(10));
					Assert.assertEquals("DurationMax[us]", nextRec.get(11));
					Assert.assertEquals("LatencyAvg[us]", nextRec.get(12));
					Assert.assertEquals("LatencyMin[us]", nextRec.get(13));
					Assert.assertEquals("LatencyMax[us]", nextRec.get(14));
					Assert.assertEquals("TPAvg[op/s]", nextRec.get(15));
					Assert.assertEquals("TPLast[op/s]", nextRec.get(16));
					Assert.assertEquals("BWAvg[MB/s]", nextRec.get(17));
					Assert.assertEquals("BWLast[MB/s]", nextRec.get(18));
					firstRow = false;
				} else {
					secondRow = true;
					Assert.assertTrue(nextRec.isConsistent());
				}
			}
		}
		Assert.assertTrue("Average metrics record was not found in the log file", secondRow);
	}
	//
	@Test public void checkFileSumMetricsLogging()
		throws Exception {
		boolean firstRow = true, secondRow = false;
		Assert.assertTrue("Performance sum metrics log file doesn't exist", FILE_LOG_PERF_SUM.exists());
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_PERF_SUM.toPath(), StandardCharsets.UTF_8)
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
					firstRow = false;
				} else  {
					secondRow = true;
					Assert.assertTrue(nextRec.isConsistent());
				}
			}
		}
		Assert.assertTrue("Summary metrics record was not found in the log file", secondRow);
	}
}
