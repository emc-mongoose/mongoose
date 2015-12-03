package com.emc.mongoose.integ.feature.circularity;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.core.impl.data.model.ItemCSVFileDst;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.util.client.api.StorageClient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_AVG;
import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_SUM;


/**
 * Created by gusakk on 30.10.15.
 */
public class CircularUpdateTest
extends StandaloneClientTestBase {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private static final int ITEM_MAX_QUEUE_SIZE = 65536;
	private static final int BATCH_SIZE = 100;
	private static final String DATA_SIZE = "128B";
	//
	private static final int WRITE_COUNT = 1234;
	private static final int UPDATE_COUNT = 24680;
	//
	private static final int COUNT_OF_UPDATES = 20;
	private static final int LAYER_NUMBER_INDEX = 2;
	//
	private static long COUNT_WRITTEN, COUNT_UPDATED;
	private static byte[] STD_OUT_CONTENT;
	//
	@BeforeClass
	public static void setUpClass() {
		try {
			System.setProperty(
				RunTimeConfig.KEY_RUN_ID, CircularUpdateTest.class.getCanonicalName()
			);
			StandaloneClientTestBase.setUpClass();
			//
			final RunTimeConfig rtConfig = RunTimeConfig.getContext();
			rtConfig.set(RunTimeConfig.KEY_LOAD_CIRCULAR, true);
			rtConfig.set(RunTimeConfig.KEY_ITEM_QUEUE_MAX_SIZE, ITEM_MAX_QUEUE_SIZE);
			rtConfig.set(RunTimeConfig.KEY_ITEM_SRC_BATCH_SIZE, BATCH_SIZE);
			RunTimeConfig.setContext(rtConfig);
			//
			try (
				final StorageClient<WSObject> client = CLIENT_BUILDER
					.setAPI("s3")
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(WRITE_COUNT)
					.build()
			) {
				final ItemDst<WSObject> writeOutput = new ItemCSVFileDst<WSObject>(
					BasicWSObject.class, ContentSourceBase.getDefault()
				);
				COUNT_WRITTEN = client.write(
					null, writeOutput, WRITE_COUNT, 10, SizeUtil.toSize(DATA_SIZE)
				);
				TimeUnit.SECONDS.sleep(1);
				RunIdFileManager.flushAll();
				//
				try (
					final BufferingOutputStream
						stdOutInterceptorStream = StdOutUtil.getStdOutBufferingStream()
				) {
					stdOutInterceptorStream.reset();
					if (COUNT_WRITTEN > 0) {
						COUNT_UPDATED = client.update(writeOutput.getItemSrc(), null, UPDATE_COUNT, 10, 1);
					} else {
						throw new IllegalStateException("Failed to update");
					}
					TimeUnit.SECONDS.sleep(1);
					STD_OUT_CONTENT = stdOutInterceptorStream.toByteArray();
				}
			}
			//
			RunIdFileManager.flushAll();
		} catch (final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed");
		}
	}
	//
	@AfterClass
	public static void tearDownClass() {
		try {
			StdOutUtil.reset();
			StandaloneClientTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed");
		}
	}
	//
	@Test
	public void checkUpdatedCount()
	throws Exception {
		Assert.assertEquals(COUNT_WRITTEN * COUNT_OF_UPDATES, COUNT_UPDATED);
	}
	//
	@Test
	public void checkLayerNumberIndex()
	throws Exception {
		try (
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_DATA_ITEMS.toPath(), StandardCharsets.UTF_8)
		) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for (final CSVRecord nextRec : recIter) {
				Assert.assertEquals(
					LAYER_NUMBER_INDEX, Integer.parseInt(nextRec.get(3).split("/")[0])
				);
			}
		}
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
					m = CONSOLE_METRICS_AVG.matcher(nextStdOutLine);
					if(m.find()) {
						Assert.assertTrue(
							"Load type is not " + IOTask.Type.UPDATE.name() + ": " + m.group("typeLoad"),
							IOTask.Type.UPDATE.name().equalsIgnoreCase(m.group("typeLoad"))
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
					m = CONSOLE_METRICS_SUM.matcher(nextStdOutLine);
					if(m.find()) {
						Assert.assertTrue(
							"Load type is not " + IOTask.Type.UPDATE.name() + ": " + m.group("typeLoad"),
							IOTask.Type.UPDATE.name().equalsIgnoreCase(m.group("typeLoad"))
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
}
