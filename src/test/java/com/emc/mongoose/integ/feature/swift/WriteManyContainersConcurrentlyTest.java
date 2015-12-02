package com.emc.mongoose.integ.feature.swift;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.run.scenario.runner.ScriptMockRunner;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 22.10.15.
 */
public class WriteManyContainersConcurrentlyTest
extends WSMockTestBase {
	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final int LIMIT_COUNT = 1000000;
	private static String RUN_ID = WriteManyContainersConcurrentlyTest.class.getCanonicalName();

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_ITEM_CLASS, "container");
		System.setProperty(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, "1");
		WSMockTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.setProperty(RunTimeConfig.KEY_API_NAME, "swift");
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, TestConstants.LOAD_CREATE);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT));
		rtConfig.set(RunTimeConfig.KEY_CREATE_CONNS, "100");
		RunTimeConfig.setContext(rtConfig);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//
		try(
			final BufferingOutputStream stdOutStream = StdOutUtil
				.getStdOutBufferingStream()
		) {
			//  Run mongoose default scenario in standalone mode
			new ScriptMockRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(1);
			STD_OUTPUT_STREAM = stdOutStream;
		}
		//
		RunIdFileManager.flushAll();
	}

	@AfterClass
	public  static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
		System.setProperty(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, "1000000");
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsToConsole()
	throws Exception {
		Assert.assertTrue(
			"Console doesn't contain information about summary metrics",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SUMMARY_INDICATOR)
		);
		Assert.assertTrue("Console doesn't contain information about end of scenario",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogValidator.getMessageFile(RUN_ID).toPath();
		//  Check that messages.log exists
		Assert.assertTrue("messages.log file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfAvgFile(RUN_ID).toPath();
		//  Check that perf.avg.csv file exists
		Assert.assertTrue("perf.avg.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfSumFile(RUN_ID).toPath();
		//  Check that perf.sum.csv file exists
		Assert.assertTrue("perf.sum.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfTraceFile(RUN_ID).toPath();
		//  Check that perf.trace.csv file exists
		Assert.assertTrue("perf.trace.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getItemsListFile(RUN_ID).toPath();
		//  Check that items list file exists
		Assert.assertTrue("items list file doesn't exist", Files.exists(expectedFile));
	}

	@Test
	public void shouldCustomValuesDisplayedCorrectlyInConfigurationTable()
	throws Exception {
		final String configTable = RunTimeConfig.getContext().toString();
		final Set<String> params = new HashSet<>();
		//  skip table header
		int start = 126;
		int lineOffset = 100;
		while (start + lineOffset < configTable.length()) {
			params.add(configTable.substring(start, start + lineOffset));
			start += lineOffset;
		}
		for (final String confParam : params) {
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_COUNT)) {
				Assert.assertTrue(
					"Information about limit count in configuration table is wrong",
					confParam.contains(String.valueOf(LIMIT_COUNT))
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(
					"Information about storage address in configuration table is wrong",
					confParam.contains("127.0.0.1")
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(
					"Information about run mode in configuration table is wrong",
					confParam.contains(Constants.RUN_MODE_STANDALONE)
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_ID)) {
				if (RUN_ID.length() >= 64) {
					Assert.assertTrue(
						"Information about run id in configuration table is wrong",
						confParam.contains(RUN_ID.substring(0, 63).trim())
					);
				} else {
					Assert.assertTrue(
						"Information about run id in configuration table is wrong",
						confParam.contains(RUN_ID)
					);
				}
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(
					"Information about limit time in configuration table is wrong",
					confParam.contains("0")
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_SCENARIO_NAME)) {
				Assert.assertTrue(
					"Information about scenario name in configuration table is wrong",
					confParam.contains(TestConstants.SCENARIO_SINGLE)
				);
			}
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		//  Read perf.summary file
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);

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
						"Count of success is not integer", LogValidator.isInteger(nextRec.get(7))
					);
					Assert.assertEquals(
						"Count of success isn't correct",
						LIMIT_COUNT, Integer.parseInt(nextRec.get(7)), LIMIT_COUNT / 10
					);
				}
			}
		}
	}

	@Test
	public void shouldCreateItemsFileWithInformationAboutAllItems()
	throws Exception {
		//  Read data.items.csv file
		final File itemsListFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("items list file doesn't exist", itemsListFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(itemsListFile.toPath(), StandardCharsets.UTF_8)
		) {
			//
			int countDataItems = 0;
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				countDataItems ++;
			}
			//  Check that there are 10 lines in data.items.csv file
			Assert.assertEquals(
				"Not correct information about created data items",
				LIMIT_COUNT, countDataItems, LIMIT_COUNT / 10
			);
		}
	}

	@Test
	public void shouldCreateCorrectItemsListFile()
	throws Exception {
		//  Get data.items.csv file
		final File itemsListFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("items list file doesn't exist", itemsListFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(itemsListFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectContainerItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		//  Get perf.sum.csv file
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file doesn't exist", perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFile()
	throws Exception {
		//  Get perf.avg.csv file
		final File perfAvgFile = LogValidator.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("perfAvg.csv file doesn't exist", perfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfAvgCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
	throws Exception {
		//  Get perf.trace.csv file
		final File perfTraceFile = LogValidator.getPerfTraceFile(RUN_ID);
		Assert.assertTrue("perf.trace.csv file doesn't exist", perfTraceFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfTraceFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfTraceCSV(in);
		}
	}

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
					Assert.fail("Duplicate item \"" + nextLine + "\" at line #" + lineNum);
				}
				lineNum ++;
			}
		}
	}
}
