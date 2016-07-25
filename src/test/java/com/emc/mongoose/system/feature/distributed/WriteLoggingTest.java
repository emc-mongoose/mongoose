package com.emc.mongoose.system.feature.distributed;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.system.base.DistributedClientTestBase;
import com.emc.mongoose.system.tools.StdOutUtil;
import static com.emc.mongoose.system.tools.LogPatterns.*;
//
import com.emc.mongoose.system.tools.BufferingOutputStream;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
//
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
/**
 Created by kurila on 16.07.15.
 */
public class WriteLoggingTest
extends DistributedClientTestBase {
	//
	private final static long COUNT_LIMIT = 1000;
	private final static String RUN_ID = WriteLoggingTest.class.getCanonicalName();
	//
	private static long countWritten;
	private static byte stdOutContent[];
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		DistributedClientTestBase.setUpClass();
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_LIMIT)
				.setAPI("swift")
				.setNamespace("swift")
				.build()
		) {
			try(
				final BufferingOutputStream
					stdOutInterceptorStream = StdOutUtil.getStdOutBufferingStream()
			) {
				countWritten = client.create(null, COUNT_LIMIT, 10, SizeInBytes.toFixedSize("10KB"));
				TimeUnit.SECONDS.sleep(10);
				stdOutContent = stdOutInterceptorStream.toByteArray();
			}
			//
			RunIdFileManager.flushAll();
			TimeUnit.SECONDS.sleep(10);
		}
	}
	//
	@Test public void checkWrittenCount()
	throws Exception {
		Assert.assertEquals(COUNT_LIMIT, countWritten);
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
							"Load type is not " + LoadType.CREATE.name(),
							LoadType.CREATE.name().toLowerCase().equals(
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
	@Test public void checkConsoleSumMetricsLogging()
	throws Exception {
		boolean passed = false;
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
						Assert.assertTrue(
							"Load type is not " + LoadType.CREATE.name(),
							LoadType.CREATE.name().toLowerCase().equals(
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
					Assert.assertEquals("Size", nextRec.get(9));
					Assert.assertEquals("JobDuration[s]", nextRec.get(10));
					Assert.assertEquals("DurationSum[s]", nextRec.get(11));
					Assert.assertEquals("TPAvg[op/s]", nextRec.get(12));
					Assert.assertEquals("TPLast[op/s]", nextRec.get(13));
					Assert.assertEquals("BWAvg[MB/s]", nextRec.get(14));
					Assert.assertEquals("BWLast[MB/s]", nextRec.get(15));
					Assert.assertEquals("DurationAvg[us]", nextRec.get(16));
					Assert.assertEquals("DurationMin[us]", nextRec.get(17));
					Assert.assertEquals("DurationMax[us]", nextRec.get(18));
					Assert.assertEquals("LatencyAvg[us]", nextRec.get(19));
					Assert.assertEquals("LatencyMin[us]", nextRec.get(20));
					Assert.assertEquals("LatencyMax[us]", nextRec.get(21));
					firstRow = false;
				} else {
					secondRow = true;
					Assert.assertTrue(
						"Load type is \"" + nextRec.get(3) + "\" but \"Create\" is expected",
						LoadType.CREATE.name().equalsIgnoreCase(nextRec.get(3))
					);
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
					Assert.assertEquals("Size", nextRec.get(9));
					Assert.assertEquals("JobDuration[s]", nextRec.get(10));
					Assert.assertEquals("DurationSum[s]", nextRec.get(11));
					Assert.assertEquals("TPAvg[op/s]", nextRec.get(12));
					Assert.assertEquals("TPLast[op/s]", nextRec.get(13));
					Assert.assertEquals("BWAvg[MB/s]", nextRec.get(14));
					Assert.assertEquals("BWLast[MB/s]", nextRec.get(15));
					Assert.assertEquals("DurationAvg[us]", nextRec.get(17));
					Assert.assertEquals("DurationMin[us]", nextRec.get(18));
					Assert.assertEquals("DurationLoQ[us]", nextRec.get(19));
					Assert.assertEquals("DurationMed[us]", nextRec.get(20));
					Assert.assertEquals("DurationHiQ[us]", nextRec.get(21));
					Assert.assertEquals("DurationMax[us]", nextRec.get(22));
					Assert.assertEquals("LatencyAvg[us]", nextRec.get(23));
					Assert.assertEquals("LatencyMin[us]", nextRec.get(24));
					Assert.assertEquals("LatencyLoQ[us]", nextRec.get(25));
					Assert.assertEquals("LatencyMed[us]", nextRec.get(26));
					Assert.assertEquals("LatencyHiQ[us]", nextRec.get(27));
					Assert.assertEquals("LatencyMax[us]", nextRec.get(28));
					firstRow = false;
				} else {
					secondRow = true;
					Assert.assertTrue(
						"Load type is \"" + nextRec.get(3) + "\" but \"Create\" is expected",
						LoadType.CREATE.name().equalsIgnoreCase(nextRec.get(3))
					);
				}
			}
		}
		Assert.assertTrue("Summary metrics record was not found in the log file", secondRow);
	}
	//
	@Test
	public void checkDataItemsAreAggregatedByClient() {
		final File dataItemsFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
	}
	//
	@Test
	public void checkNoItemDuplicatesLogged()
	throws Exception {
		final Set<String> items = new TreeSet<>();
		String nextLine;
		int lineNum = 0;
		try(
			final BufferedReader in = Files.newBufferedReader(
				LogValidator.getItemsListFile(RUN_ID).toPath(), StandardCharsets.UTF_8
			)
		) {
			while((nextLine = in.readLine()) != null) {
				if(!items.add(nextLine)) {
					Assert.fail("Duplicate item \"" + nextLine + "\" at line #" + lineNum);
				}
				lineNum ++;
			}
		}
	}
}
