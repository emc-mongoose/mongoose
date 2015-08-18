package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.run.scenario.ScriptRunner;
import com.emc.mongoose.storage.adapter.s3.Bucket;
import com.emc.mongoose.storage.adapter.s3.WSBucketImpl;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 09.07.15.
 * Covers TC #6(name: "Limit the single write load job w/ both data item count and timeout",
 * steps: all, dominant limit: count) in Mongoose Core Functional Testing
 * HLUC: 1.1.5.2, 1.1.5.5
 */
public class WriteByCountTest
extends WSMockTestBase {

	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final String RUN_ID = WriteByCountTest.class.getCanonicalName();
	private static final String DATA_SIZE = "1B", LIMIT_TIME = "365.days";
	private static final int LIMIT_COUNT = 100000, LOAD_THREADS = 10;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT));
		System.setProperty(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		System.setProperty(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		System.setProperty(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
		System.setProperty(RunTimeConfig.KEY_LOAD_TYPE_CREATE_THREADS, Integer.toString(LOAD_THREADS));
		System.setProperty(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		WSMockTestBase.setUpClass();
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//
		try (final BufferingOutputStream
				 stdOutStream =	StdOutInterceptorTestSuite.getStdOutBufferingStream()
		) {
			UniformDataSource.DEFAULT = new UniformDataSource();
			//  Run mongoose default scenario in standalone mode
			new ScriptRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(5);
			STD_OUTPUT_STREAM = stdOutStream;
		}
		STD_OUTPUT_STREAM.close();
		//
		RunIdFileManager.flushAll();
	}

	@AfterClass
	public  static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsToConsole()
	throws Exception {
		Assert.assertTrue("Console doesn't contain information about summary metrics",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SUMMARY_INDICATOR)
		);
		Assert.assertTrue("Console doesn't contain information about end of scenario",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//  Read the message file and search for "Scenario end"
		final File messageFile = LogParser.getMessageFile(RUN_ID);
		Assert.assertTrue(
			"messages.log file doesn't exist",
			messageFile.exists()
		);
		//
		try (final BufferedReader bufferedReader =
				 new BufferedReader(new FileReader(messageFile))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TestConstants.SCENARIO_END_INDICATOR)) {
					break;
				}
			}
			Assert.assertNotNull(
				"Line with information about end of scenario must not be equal null ", line
			);
			Assert.assertTrue(
				"Information about end of scenario doesn't contain in message.log file",
				line.contains(TestConstants.SCENARIO_END_INDICATOR)
			);
		}
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(RUN_ID).toPath();
		//  Check that messages.log exists
		Assert.assertTrue("messages.log file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(RUN_ID).toPath();
		//  Check that perf.avg.csv file exists
		Assert.assertTrue("perf.avg.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(RUN_ID).toPath();
		//  Check that perf.sum.csv file exists
		Assert.assertTrue("perf.sum.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(RUN_ID).toPath();
		//  Check that perf.trace.csv file exists
		Assert.assertTrue("perf.trace.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(RUN_ID).toPath();
		//  Check that data.items.csv file exists
		Assert.assertTrue("data.items.csv file doesn't exist", Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
		throws Exception {
		//  Read data.items.csv file
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue("data.items.csv file doesn't exist", dataItemsFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemsFile.toPath(), StandardCharsets.UTF_8)
		) {
			//
			int countDataItems = 0;
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				Assert.assertEquals(
					"Size of data item isn't correct",
					Long.toString(SizeUtil.toSize(DATA_SIZE)), nextRec.get(2)
				);
				countDataItems++;
			}
			//  Check that there are 10 lines in data.items.csv file
			Assert.assertEquals(
				"Not correct information about created data items", LIMIT_COUNT, countDataItems
			);
		}
	}

	@Test
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		// Get data.items.csv file of write scenario run
		final File dataItemFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue("data.items.csv file doesn't exist", dataItemFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectDataItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file of write scenario run
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file doesn't exist", perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		//  Read perf.summary file
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);

		//  Check that file exists
		Assert.assertTrue("perf.sum.csv file doesn't exist", perfSumFile.exists());

		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			//
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if (firstRow) {
					firstRow = false;
				} else if (nextRec.size() == 21) {
					Assert.assertTrue(
						"Count of success is not integer", LogParser.isInteger(nextRec.get(7))
					);
					Assert.assertEquals(
						"Count of success isn't correct", Integer.toString(LIMIT_COUNT), nextRec.get(7)
					);
				}
			}
		}
	}
}
