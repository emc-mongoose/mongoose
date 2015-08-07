package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
//
import com.emc.mongoose.run.scenario.ScriptRunner;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 07.07.15.
 * Covers TC #2(name: "Read back the data items written in the different run.", steps: 3 for data.size=10MB)
 * in Mongoose Core Functional Testing
 * HLUC: 1.1.4.2
 */
public class ReadVerificationTest {
	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final int LIMIT_COUNT = 10;
	private static final String DATA_SIZE = "10B";
	private static final String RUN_ID = ReadVerificationTest.class.getCanonicalName();
	private static final String WRONG_SEED = "7a42d9c483244166";

	private static final String
		CREATE_RUN_ID = RUN_ID + TestConstants.LOAD_CREATE,
		READ_RUN_ID = RUN_ID + TestConstants.LOAD_READ;

	private static Logger LOG;

	@BeforeClass
	public static void before()
	throws Exception{
		//  remove log dir w/ previous logs
		LogParser.removeLogDirectory(CREATE_RUN_ID);
		LogParser.removeLogDirectory(READ_RUN_ID);
		//
		RunTimeConfig.setContext(RunTimeConfig.getDefault());
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, CREATE_RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		LoggingTestSuite.setUpClass();

		LOG = LogManager.getLogger();
		//  write
		executeLoadJob(rtConfig);
		//  reset before using
		StdOutInterceptorTestSuite.reset();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, READ_RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_DATA_SRC_FPATH, LogParser
			.getDataItemsFile(CREATE_RUN_ID).getPath());
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, TestConstants.LOAD_READ);
		rtConfig.set(RunTimeConfig.KEY_DATA_RING_SEED, WRONG_SEED);
		//  read
		executeLoadJob(rtConfig);
		STD_OUTPUT_STREAM.close();
	}

	private static void executeLoadJob(final RunTimeConfig rtConfig)
	throws Exception {
		LOG.info(Markers.MSG, rtConfig.toString());
		UniformDataSource.DEFAULT = new UniformDataSource();
		try (final BufferingOutputStream stdOutStream =
		        StdOutInterceptorTestSuite.getStdOutBufferingStream()) {
			//  Run mongoose default scenario in standalone mode
			new ScriptRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(5);
			STD_OUTPUT_STREAM = stdOutStream;
		}
	}

	@Test
	public void shouldFailedReadOfAllDataItems()
	throws Exception {
		// Get perf.sum.csv file of read scenario
		final File perfSumFile = LogParser.getPerfSumFile(READ_RUN_ID);
		Assert.assertTrue(perfSumFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(perfSumFile))) {
			//  read header of csv file
			bufferedReader.readLine();
			int countFail = Integer.valueOf(
				bufferedReader.readLine().split(",")[TestConstants.COUNT_FAIL_COLUMN_INDEX]
			);
			Assert.assertEquals(LIMIT_COUNT, countFail);
		}
	}

	@Test
	public void shouldReportAboutFailedVerificationToConsole()
	throws Exception {
		// Get data.items.csv file of write scenario
		final File dataItemsFile = LogParser.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			String line, dataID;
			while ((line = bufferedReader.readLine()) != null) {
				dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
				Assert.assertTrue(
					STD_OUTPUT_STREAM.toString()
						.contains(dataID + TestConstants.CONTENT_MISMATCH_INDICATOR)
				);
			}
		}
	}

	@Test
	public void shouldReportAboutFailedVerificationToMessageFile()
	throws Exception {
		// Get data.items.csv file of write scenario
		final File dataItemsFile = LogParser.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedDataItemsReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			// Get content of message.log file of read scenario
			final String contentMessageFile = new Scanner(LogParser.getMessageFile(READ_RUN_ID))
				.useDelimiter("\\Z")
				.next();
			String line, dataID;
			while ((line = bufferedDataItemsReader.readLine()) != null) {
				dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
				Assert.assertTrue(
					contentMessageFile.contains(dataID + TestConstants.CONTENT_MISMATCH_INDICATOR)
				);
			}
		}
	}
}
