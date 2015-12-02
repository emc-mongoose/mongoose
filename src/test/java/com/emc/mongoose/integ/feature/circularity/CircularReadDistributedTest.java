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
import com.emc.mongoose.integ.base.DistributedClientTestBase;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_AVG_CLIENT;
import static com.emc.mongoose.integ.tools.LogPatterns.CONSOLE_METRICS_SUM_CLIENT;

/**
 * Created by gusakk on 07.10.15.
 */
public class CircularReadDistributedTest
extends DistributedClientTestBase {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private static final int ITEM_MAX_QUEUE_SIZE = 65536;
	private static final int BATCH_SIZE = 100;
	private static final String DATA_SIZE = "128B";
	//
	private static final int WRITE_COUNT = 1234;
	private static final int READ_COUNT = 12340;
	//
	private static final int COUNT_OF_DUPLICATES = 11;
	//
	private static byte[] STD_OUT_CONTENT;
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	private static final String RUN_ID = CircularReadDistributedTest.class.getCanonicalName();
	//
	@BeforeClass
	public static void setUpClass() {
		try {
			System.setProperty(
				RunTimeConfig.KEY_RUN_ID, RUN_ID
			);
			DistributedClientTestBase.setUpClass();
			//
			final RunTimeConfig rtConfig = RunTimeConfig.getContext();
			rtConfig.set(RunTimeConfig.KEY_ITEM_SRC_CIRCULAR, true);
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
						COUNT_READ = client.read(writeOutput.getItemSrc(), null, READ_COUNT, 10, true);
					} else {
						throw new IllegalStateException("Failed to read");
					}
					TimeUnit.SECONDS.sleep(1);
					STD_OUT_CONTENT = stdOutInterceptorStream.toByteArray();
				}
			}
			//
			RunIdFileManager.flushAll();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed");
		}
	}
	//
	@AfterClass
	public static void tearDownClass() {
		try {
			StdOutUtil.reset();
			DistributedClientTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed");
		}
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
	public void checkPerfTraceFileContainsDuplicates()
	throws  Exception {
		final Map<String, Long> items = new HashMap<>();
		boolean firstRow = true;
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_PERF_TRACE.toPath(), StandardCharsets.UTF_8)
		) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			String id;
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
				} else {
					long count = 1;
					id = nextRec.get(2);
					if(items.containsKey(id)) {
						count = items.get(id);
						count++;
					}
					items.put(id, count);
				}
			}
			//
			Assert.assertEquals("Data haven't been read fully", items.size(), WRITE_COUNT);
			for(final Map.Entry<String, Long> entry : items.entrySet()) {
				Assert.assertEquals(
					"perf.trace.csv doesn't contain necessary count of duplicated items" ,
					entry.getValue(), Long.valueOf(COUNT_OF_DUPLICATES), 2);
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
					m = CONSOLE_METRICS_AVG_CLIENT.matcher(nextStdOutLine);
					if(m.find()) {
						Assert.assertTrue(
							"Load type is not " + IOTask.Type.READ.name() + ": " + m.group("typeLoad"),
							IOTask.Type.READ.name().equalsIgnoreCase(m.group("typeLoad"))
						);
						long
							nextSuccCount = Long.parseLong(m.group("countSucc")),
							nextFailCount = Long.parseLong(m.group("countFail"));
						Assert.assertTrue(
							"Next read items count " + nextSuccCount +
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
							"Load type is not " + IOTask.Type.READ.name() + ": " + m.group("typeLoad"),
							IOTask.Type.READ.name().equalsIgnoreCase(m.group("typeLoad"))
						);
						long countFail = Long.parseLong(m.group("countFail"));
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
