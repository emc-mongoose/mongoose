package com.emc.mongoose.system.feature.distributed;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.base.ListItemOutput;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
import com.emc.mongoose.system.tools.BufferingOutputStream;
import com.emc.mongoose.system.tools.StdOutUtil;
import com.emc.mongoose.util.client.api.StorageClient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.system.tools.LogPatterns.CONSOLE_METRICS_MED;

/**
 Created by kurila on 28.06.16.
 */
public class ReadRandomSizeDistributedTest
extends StandaloneClientTestBase {

	private final static long ITEM_COUNT = 345;
	private final static long ITEM_SIZE_MAX = SizeInBytes.toFixedSize("200MB");
	private final static int CONCURRENCY_LEVEL = 200;
	private final static String RUN_ID = ReadRandomSizeDistributedTest.class.getCanonicalName();

	private static long CREATED_ACTUALLY_COUNT;
	private static long READ_ACTUALLY_COUNT;
	private static byte STD_OUT_CONTENT[];

	@BeforeClass
	public static void setUpClass() {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(AppConfig.KEY_LOAD_METRICS_INTERMEDIATE, "true");
		try {
			StandaloneClientTestBase.setUpClass();
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER.build()
		) {
			try(final BufferingOutputStream stdOutStream = StdOutUtil.getStdOutBufferingStream()) {
				try(
					final Output<HttpDataItem> itemOutput = new ListItemOutput<>(
						new ArrayList<HttpDataItem>()
					)
				) {
					CREATED_ACTUALLY_COUNT = client.create(
						itemOutput, ITEM_COUNT, CONCURRENCY_LEVEL, 0, ITEM_SIZE_MAX, 1
					);
					TimeUnit.SECONDS.sleep(10);
					READ_ACTUALLY_COUNT = client.read(
						itemOutput.getInput(), null, ITEM_COUNT, CONCURRENCY_LEVEL, true
					);
					TimeUnit.SECONDS.sleep(10);
					RunIdFileManager.closeAll(RUN_ID);
					STD_OUT_CONTENT = stdOutStream.toByteArray();
				}
			}
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
	}

	@Test public void checkConsoleSumMetricsLogging()
	throws Exception {
		int passed = 0;
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
					m = CONSOLE_METRICS_MED.matcher(nextStdOutLine);
					if(m.find()) {
						long
							countLimit = Long.parseLong(m.group("countLimit")),
							countSucc = Long.parseLong(m.group("countSucc")),
							countFail = Long.parseLong(m.group("countFail"));
						Assert.assertTrue(
							"Written items count " + countSucc +
								" is not equal to the limit: " + countLimit,
							countSucc < countLimit
						);
						Assert.assertTrue("There are failures reported", countFail == 0);
						passed ++;
					}
				}
			} while(true);
		}
		Assert.assertTrue(
			"Summary metrics line matching the pattern was not met in the stdout", passed == 2
		);
	}

	@Test public void checkFileMedMetricsLogging()
	throws Exception {
		boolean firstRow = true, secondRow = false;
		Assert.assertTrue("Performance med metrics log file doesn't exist", FILE_LOG_PERF_SUM.exists());
		try(
			final BufferedReader in = Files.newBufferedReader(
				FILE_LOG_PERF_MED.toPath(), StandardCharsets.UTF_8
			)
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
				} else {
					secondRow = true;
				}
			}
		}
		Assert.assertTrue("Summary metrics record was not found in the log file", secondRow);
	}
}
