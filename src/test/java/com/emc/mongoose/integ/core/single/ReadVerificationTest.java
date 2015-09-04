package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.base.LoggingTestBase;
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
//
import com.emc.mongoose.run.scenario.ScriptRunner;
//
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 07.07.15.
 * Covers TC #2(name: "Read back the data items written in the different run.", steps: 3 for data.size=10MB)
 * in Mongoose Core Functional Testing
 * HLUC: 1.1.4.2
 */
public class ReadVerificationTest
extends WSMockTestBase {

	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final int LIMIT_COUNT = 10;
	private static final String DATA_SIZE = "10B";
	private static final String RUN_ID = ReadVerificationTest.class.getCanonicalName();
	private static final String WRONG_SEED = "7a42d9c483244166";

	private static final String
		CREATE_RUN_ID = RUN_ID + TestConstants.LOAD_CREATE,
		READ_RUN_ID = RUN_ID + TestConstants.LOAD_READ;

	@BeforeClass
	public static void setUpClass()
	throws Exception{
		System.setProperty(RunTimeConfig.KEY_RUN_ID, CREATE_RUN_ID);
		WSMockTestBase.setUpClass();
		//
		RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT));
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		RunTimeConfig.setContext(rtConfig);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//  write
		UniformDataSource.DEFAULT = new UniformDataSource();
		new ScriptRunner().run();
		//
		RunIdFileManager.flushAll();
		//
		System.setProperty(RunTimeConfig.KEY_RUN_ID, READ_RUN_ID);
		LoggingTestBase.setUpClass();
		//
		rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_DATA_SRC_FPATH,
			LogParser.getDataItemsFile(CREATE_RUN_ID).getPath());
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, TestConstants.LOAD_READ);
		rtConfig.set(RunTimeConfig.KEY_DATA_SRC_RING_SEED, WRONG_SEED);
		rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		RunTimeConfig.setContext(rtConfig);
		//
		logger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//  read
		UniformDataSource.DEFAULT = new UniformDataSource();
		try (final BufferingOutputStream
				 stdOutStream = StdOutInterceptorTestSuite.getStdOutBufferingStream()
		) {
			new ScriptRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(5);
			STD_OUTPUT_STREAM = stdOutStream;
		}
		//
		RunIdFileManager.flushAll();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
	}

	@Test
	public void shouldFailedReadOfAllDataItems()
	throws Exception {
		//  Read perf.summary file
		final File perfSumFile = LogParser.getPerfSumFile(READ_RUN_ID);

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
						"Count of failed data items is not integer", LogParser.isInteger(nextRec.get(8))
					);
					Assert.assertEquals(
						"Count of failed data items isn't correct", Integer.toString(LIMIT_COUNT), nextRec.get(8)
					);
				}
			}
		}
	}

	@Test
	public void shouldReportAboutFailedVerificationToConsole()
	throws Exception {
		// Get data.items.csv file of write scenario
		final File dataItemsFile = LogParser.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue("data.items.csv file of create load doesn't exist", dataItemsFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemsFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			//
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if (firstRow) {
					firstRow = false;
				} else if (nextRec.size() == 21) {
					Assert.assertTrue(
						"Console doesn't produce information about errors",
						STD_OUTPUT_STREAM.toString()
							.contains(nextRec.get(0) + TestConstants.CONTENT_MISMATCH_INDICATOR)
					);
				}
			}
		}
	}

	@Test
	public void shouldReportAboutFailedVerificationToMessageFile()
	throws Exception {
		// Get data.items.csv file of write scenario
		final File dataItemsFile = LogParser.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue("data.items.csv file of create load doesn't exist", dataItemsFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemsFile.toPath(), StandardCharsets.UTF_8)
		) {
			// Get content of message.log file of read scenario
			final String contentMessageFile = new Scanner(LogParser.getMessageFile(READ_RUN_ID))
				.useDelimiter("\\Z")
				.next();
			boolean firstRow = true;
			//
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if (firstRow) {
					firstRow = false;
				} else if (nextRec.size() == 21) {
					Assert.assertTrue(
						"There isn't information about errors in message.log file",
						contentMessageFile.contains(nextRec.get(0) + TestConstants.CONTENT_MISMATCH_INDICATOR)
					);
				}
			}
		}
	}

	@Test
	public void shouldReportToMessageFileAboutAllFailedVerification()
	throws Exception {
		// Get message.log file of write scenario
		final File messageFile = LogParser.getMessageFile(READ_RUN_ID);
		Assert.assertTrue("message.log file of read load doesn't exist", messageFile.exists());
		//
		try (final BufferedReader
			 bufferedReader = new BufferedReader(new FileReader(messageFile))
		) {
			int countReports = 0;
			String line = bufferedReader.readLine();
			//
			while (line != null) {
				if (line.contains(TestConstants.CONTENT_MISMATCH_INDICATOR)) {
					countReports++;
				}
				line = bufferedReader.readLine();
			}
			Assert.assertEquals(
				"Count of failed verification isn't correct", LIMIT_COUNT, countReports
			);
		}
	}
}
