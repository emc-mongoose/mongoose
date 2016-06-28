package com.emc.mongoose.system.feature.distributed;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.system.base.DistributedLoadBuilderTestBase;
import com.emc.mongoose.system.tools.StdOutUtil;
import com.emc.mongoose.system.tools.BufferingOutputStream;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.system.tools.TestConstants;
import com.emc.mongoose.run.scenario.runner.ScenarioRunner;
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
public class WriteManyBucketsConcurrentlyDistributedTest
extends DistributedLoadBuilderTestBase {
	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final int LIMIT_COUNT = 100000;
	private static String RUN_ID = WriteManyBucketsConcurrentlyDistributedTest.class.getCanonicalName();

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(AppConfig.KEY_ITEM_TYPE, "container");
		System.setProperty(AppConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, "1");
		DistributedLoadBuilderTestBase.setUpClass();
		final AppConfig rtConfig = BasicConfig.THREAD_CONTEXT.get();
		rtConfig.setProperty(AppConfig.KEY_LOAD_TYPE, TestConstants.LOAD_CREATE);
		rtConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT));
		rtConfig.setProperty(AppConfig.KEY_LOAD_THREADS, "100");
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, BasicConfig.THREAD_CONTEXT.get().toString());
		//
		try(
			final BufferingOutputStream stdOutStream = StdOutUtil
				.getStdOutBufferingStream()
		) {
			//  Run mongoose default scenario in standalone mode
			new ScenarioRunner(rtConfig).run();
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
		DistributedLoadBuilderTestBase.tearDownClass();
		System.setProperty(AppConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, "1000000");
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
		Path expectedFile = LogValidator.getMessageLogFile(RUN_ID).toPath();
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
		final String configTable = BasicConfig.THREAD_CONTEXT.get().toString();
		final Set<String> params = new HashSet<>();
		//  skip table header
		int start = 126;
		int lineOffset = 100;
		while (start + lineOffset < configTable.length()) {
			params.add(configTable.substring(start, start + lineOffset));
			start += lineOffset;
		}
		for (final String confParam : params) {
			if (confParam.contains(AppConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(
					"Information about storage address in configuration table is wrong",
					confParam.contains("127.0.0.1")
				);
			}
			if (confParam.contains(AppConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(
					"Information about run mode in configuration table is wrong",
					confParam.contains(Constants.RUN_MODE_CLIENT)
				);
			}
			if (confParam.contains(AppConfig.KEY_RUN_ID)) {
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
			if (confParam.contains(AppConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(
					"Information about limit time in configuration table is wrong",
					confParam.contains("0")
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
						"Count of success isn't correct", Integer.toString(LIMIT_COUNT), nextRec.get(7)
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
				LIMIT_COUNT, countDataItems, LIMIT_COUNT / 1000
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
				}
				lineNum ++;
			}
		}
	}
}
