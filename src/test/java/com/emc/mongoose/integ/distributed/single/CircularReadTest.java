package com.emc.mongoose.integ.distributed.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.core.impl.data.model.CSVFileItemDst;
import com.emc.mongoose.integ.base.DistributedClientTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
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
public class CircularReadTest
extends DistributedClientTestBase {
	//
	private static final int WRITE_COUNT = 1234;
	private static final int READ_COUNT = 12340;
	//
	private static final int COUNT_OF_DUPLICATES = 10;
	//
	private static byte STD_OUT_CONTENT[];
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, CircularReadTest.class.getCanonicalName());
		DistributedClientTestBase.setUpClass();
		//
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_DATA_SRC_CIRCULAR, true);
		RunTimeConfig.setContext(rtConfig);
		//
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setAPI("s3")
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(WRITE_COUNT)
				.build()
		) {
			final ItemDst<WSObject> writeOutput = new CSVFileItemDst<WSObject>(
				BasicWSObject.class, ContentSourceBase.getDefault()
			);
			final long countWritten = client.write(
				null, writeOutput, WRITE_COUNT, 1, SizeUtil.toSize("1MB")
			);
			TimeUnit.SECONDS.sleep(10);
			//
			try (
				final BufferingOutputStream
					stdOutInterceptorStream = StdOutInterceptorTestSuite.getStdOutBufferingStream()
			) {
				stdOutInterceptorStream.reset();
				if (countWritten > 0) {
					 client.read(writeOutput.getItemSrc(), null, READ_COUNT, 1, true);
				} else {
					throw new IllegalStateException("Failed to write");
				}
				TimeUnit.SECONDS.sleep(1);
				STD_OUT_CONTENT = stdOutInterceptorStream.toByteArray();
			}
		}
		//
		RunIdFileManager.flushAll();
	}
	//
	@AfterClass
	public static void tearDown()
	throws Exception {
		StdOutInterceptorTestSuite.reset();
	}
	//
	@Test
	public void checkItemsFileContainsDuplicates()
	throws  Exception {
		final Map<String, Long> items = new HashMap<>();
		try(
			final BufferedReader
				in = Files.newBufferedReader(FILE_LOG_DATA_ITEMS.toPath(), StandardCharsets.UTF_8)
		) {
			String line;
			while ((line = in.readLine()) != null) {
				long count = 1;
				if (items.containsKey(line)) {
					count = items.get(line);
					count++;
				}
				items.put(line, count);
			}
			Assert.assertEquals("Data haven't been read fully", items.size(), WRITE_COUNT);
			for (final Map.Entry<String, Long> entry : items.entrySet()) {
				Assert.assertEquals(
					"data.items.csv doesn't contain necessary count of duplicated items" ,
					entry.getValue(), Long.valueOf(COUNT_OF_DUPLICATES));
			}
		}
	}
	//
	@Test
	public void checkItemDuplicatesOrder()
	throws Exception {
		final Map<String, Integer> items = new HashMap<>();
		try (
			final LineNumberReader in = new LineNumberReader(
				Files.newBufferedReader(FILE_LOG_DATA_ITEMS.toPath(), StandardCharsets.UTF_8)
			)
		) {
			long uniqueItems = 0;
			String line;
			while ((line = in.readLine()) != null) {
				if (uniqueItems < WRITE_COUNT) {
					items.put(line, in.getLineNumber());
					uniqueItems++;
				} else {
					Assert.assertTrue(items.containsKey(line));
					final int expected;
					if (in.getLineNumber() % WRITE_COUNT == 0) {
						expected = WRITE_COUNT;
					} else {
						expected = in.getLineNumber() % WRITE_COUNT;
					}
					Assert.assertEquals(
						Integer.valueOf(expected), items.get(line)
					);
				}
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
							"Next deleted items count " + nextSuccCount +
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
						long
							countLimit = Long.parseLong(m.group("countLimit")),
							countSucc = Long.parseLong(m.group("countSucc")),
							countFail = Long.parseLong(m.group("countFail"));
						Assert.assertTrue(
							"Deleted items count " + countSucc +
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
