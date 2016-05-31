package com.emc.mongoose.system.feature.loadtype;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.impl.item.data.ContentSourceUtil;
import com.emc.mongoose.system.base.LoggingTestBase;
import com.emc.mongoose.system.base.ScenarioTestBase;
import com.emc.mongoose.system.tools.StdOutUtil;
import com.emc.mongoose.system.tools.TestConstants;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.system.tools.BufferingOutputStream;
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
extends ScenarioTestBase {

	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final int LIMIT_COUNT = 10;
	private static final String DATA_SIZE = "10B";
	private static final String RUN_ID = ReadVerificationTest.class.getCanonicalName();

	private static final String
		CREATE_RUN_ID = RUN_ID + TestConstants.LOAD_CREATE,
		READ_RUN_ID = RUN_ID + TestConstants.LOAD_READ;

	@BeforeClass
	public static void setUpClass() {
		System.setProperty(AppConfig.KEY_RUN_ID, CREATE_RUN_ID);
		ScenarioTestBase.setUpClass();
		//
		AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT));
		appConfig.setProperty(AppConfig.KEY_ITEM_DATA_SIZE, DATA_SIZE);
		appConfig.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, RUN_ID);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, BasicConfig.THREAD_CONTEXT.get().toString());
		//  write
		SCENARIO_RUNNER.run();
		//
		try {
			RunIdFileManager.flushAll();
			//
			System.setProperty(AppConfig.KEY_RUN_ID, READ_RUN_ID);
			LoggingTestBase.setUpClass();
			//
			appConfig = BasicConfig.THREAD_CONTEXT.get();
			appConfig.setProperty(AppConfig.KEY_ITEM_SRC_FILE,
				LogValidator.getItemsListFile(CREATE_RUN_ID).getPath()
			);
			appConfig.setProperty(AppConfig.KEY_LOAD_TYPE, TestConstants.LOAD_READ.toLowerCase());
			appConfig.setProperty(AppConfig.KEY_ITEM_DATA_CONTENT_FILE, "conf/content/zerobytes");
			ContentSourceUtil.DEFAULT = null;
			appConfig.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, RUN_ID);
			//
			logger.info(Markers.MSG, BasicConfig.THREAD_CONTEXT.get().toString());
			//  read
			try(
				final BufferingOutputStream stdOutStream = StdOutUtil.getStdOutBufferingStream()
			) {
				SCENARIO_RUNNER.run();
				//  Wait for "Scenario end" message
				TimeUnit.SECONDS.sleep(5);
				STD_OUTPUT_STREAM = stdOutStream;
			}
			//
			RunIdFileManager.flushAll();
		} catch(final Exception e) {
			e.printStackTrace(System.err);
		}
	}

	@AfterClass
	public static void tearDownClass() {
		ScenarioTestBase.tearDownClass();
	}

	@Test
	public void shouldFailedReadOfAllDataItems()
	throws Exception {
		//  Read perf.summary file
		final File perfSumFile = LogValidator.getPerfSumFile(READ_RUN_ID);

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
				} else if (nextRec.size() == 23) {
					Assert.assertTrue(
						"Count of failed data items is not integer", LogValidator.isInteger(
							nextRec.get(8)
						)
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
		final File dataItemsFile = LogValidator.getItemsListFile(CREATE_RUN_ID);
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
		final File dataItemsFile = LogValidator.getItemsListFile(CREATE_RUN_ID);
		Assert.assertTrue("data.items.csv file of create load doesn't exist", dataItemsFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemsFile.toPath(), StandardCharsets.UTF_8)
		) {
			// Get content of message.log file of read scenario
			final String contentMessageFile = new Scanner(LogValidator.getMessageFile(READ_RUN_ID))
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
}
