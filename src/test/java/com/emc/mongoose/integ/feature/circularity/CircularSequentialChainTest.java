package com.emc.mongoose.integ.feature.circularity;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.ItemDst;
import com.emc.mongoose.core.impl.item.data.BasicHttpObject;
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
import com.emc.mongoose.core.impl.item.base.ItemCSVFileDst;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.util.client.api.StorageClient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_SUM;

/**
 * Created by gusakk on 19.11.15.
 */
public class CircularSequentialChainTest
extends StandaloneClientTestBase {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private static final int ITEM_MAX_QUEUE_SIZE = 65536;
	private static final int BATCH_SIZE = 100;
	private static final int WRITE_COUNT = 100;
	//
	private static final int RESULTS_COUNT = 6000;
	private static final String DATA_SIZE = "128B";
	private static final int LAYER_NUMBER_INDEX = 7;
	//
	private static long COUNT_WRITTEN;
	//
	private final static String
		RUN_ID = CircularSequentialChainTest.class.getCanonicalName(),
		LOAD_SEQ[] = { "read", "update" };
	private static final int COUNT_STEPS = LOAD_SEQ.length;
	//
	private static byte STD_OUT_CONTENT[] = null;
	//
	@BeforeClass
	public static void setUpClass() {
		try {
			System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
			StandaloneClientTestBase.setUpClass();
			//
			final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
			appConfig.set(RunTimeConfig.KEY_ITEM_QUEUE_MAX_SIZE, ITEM_MAX_QUEUE_SIZE);
			appConfig.set(RunTimeConfig.KEY_ITEM_SRC_BATCH_SIZE, BATCH_SIZE);
			appConfig.set(RunTimeConfig.KEY_LOAD_CIRCULAR, true);
			RunTimeConfig.setContext(appConfig);
			//
			try (
				final StorageClient<HttpDataItem> client = CLIENT_BUILDER
					.setAPI("s3")
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(WRITE_COUNT)
					.setS3Bucket(RUN_ID)
					.build()
			) {
				final ItemDst<HttpDataItem> writeOutput = new ItemCSVFileDst<HttpDataItem>(
					BasicHttpObject.class, ContentSourceBase.getDefault()
				);
				COUNT_WRITTEN = client.write(
					null, writeOutput, WRITE_COUNT, 1, SizeUtil.toSize(DATA_SIZE)
				);
				TimeUnit.SECONDS.sleep(1);
				RunIdFileManager.flushAll();
			}
			//
			RunTimeConfig newConfig = BasicConfig.THREAD_CONTEXT.get();
			newConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD, LOAD_SEQ);
			newConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT, false);
			newConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, RUN_ID);
			newConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, RESULTS_COUNT);
			RunTimeConfig.setContext(appConfig);
			//
			final Chain chainScenario = new Chain(appConfig);
			try (
				final BufferingOutputStream
					stdOutBuffer = StdOutUtil.getStdOutBufferingStream()
			) {
				chainScenario.run();
				TimeUnit.SECONDS.sleep(10);
				STD_OUT_CONTENT = stdOutBuffer.toByteArray();
			}
			//
			RunIdFileManager.flushAll();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed");
		}
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
					m = CONSOLE_METRICS_SUM.matcher(nextStdOutLine);
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
	public void checkItemsFileExists()
	throws Exception {
		final Map<String, Long> items = new HashMap<>();
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_DATA_ITEMS.toPath(), StandardCharsets.UTF_8)
		) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			String id;
			for(final CSVRecord nextRec : recIter) {
				long count = 1;
				id = nextRec.get(0);
				if(items.containsKey(id)) {
					count = items.get(id);
					count++;
				}
				items.put(id, count);
			}
			//
			Assert.assertEquals("Data haven't been read fully", items.size(), WRITE_COUNT);
		}
	}
	//
	@Test
	public void checkAllItemsAreProcessed()
	throws Exception {
		boolean firstRow = true;
		boolean secondRow = true;
		Assert.assertTrue("Performance sum metrics file doesn't exist", FILE_LOG_PERF_SUM.exists());
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_PERF_SUM.toPath(), StandardCharsets.UTF_8)
		) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
				} else {
					if(secondRow) {
						secondRow = false;
						continue;
					}
					Assert.assertEquals(Integer.parseInt(nextRec.get(7)), RESULTS_COUNT);
				}
			}
		}
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
}
