package com.emc.mongoose.system.feature.rampup;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.system.base.WSMockTestBase;
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
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 22.07.15.
 * HLUC: 1.5.3.1
 */
public class DefaultRampupTest
extends WSMockTestBase {
	//
	private static BufferingOutputStream STD_OUTPUT_STREAM;
	//
	private static String RUN_ID = DefaultRampupTest.class.getCanonicalName();
	private static final String	LIMIT_TIME = "1m";
	private static final int COUNT_STEPS = 4;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		WSMockTestBase.setUpClass();
		//
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(AppConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
		rtConfig.set(AppConfig.KEY_SCENARIO_NAME, TestConstants.SCENARIO_RAMPUP);
		rtConfig.set(AppConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		RunTimeConfig.setContext(rtConfig);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//
		try (final BufferingOutputStream
				 stdOutStream =	StdOutUtil.getStdOutBufferingStream()
		) {
			//  Run mongoose default scenario in standalone mode
			new ScenarioRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(5);
			STD_OUTPUT_STREAM = stdOutStream;
		}
		//
		RunIdFileManager.flushAll();
	}

	@AfterClass
	public  static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(
			"Should report information about end of scenario run from console",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogValidator.getMessageFile(RUN_ID).toPath();
		Assert.assertTrue("messages.log file must be contained", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfSumFile(RUN_ID).toPath();
		Assert.assertTrue("perf.sum.csv file must be contained", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfTraceFile(RUN_ID).toPath();
		Assert.assertTrue("perf.trace.csv file must be contained", Files.exists(expectedFile));

		expectedFile = LogValidator.getItemsListFile(RUN_ID).toPath();
		Assert.assertTrue("data.items.csv file must be contained", Files.exists(expectedFile));
	}

	@Test
	public void shouldCustomValuesDisplayedCorrectlyInConfigurationTable()
	throws Exception {
		final String[] runtimeConfCustomParam = RunTimeConfig.getContext().toString().split("\n");
		for (final String confParam : runtimeConfCustomParam) {
			if (confParam.contains(AppConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(
					"Information about time limit must be correct in configuration table",
					confParam.contains(LIMIT_TIME)
				);
			}
			if (confParam.contains(AppConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(
					"Information about storage address must be correct in configuration table",
					confParam.contains("127.0.0.1")
				);
			}
			if (confParam.contains(AppConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(
					"Information about run mode must be correct in configuration table",
					confParam.contains(Constants.RUN_MODE_STANDALONE)
				);
			}
			if (confParam.contains(AppConfig.KEY_RUN_ID)) {
				if (RUN_ID.length() >= 64) {
					Assert.assertTrue(confParam.contains(RUN_ID.substring(0, 63).trim()));
				} else {
					Assert.assertTrue(confParam.contains(RUN_ID));
				}
			}
			if (confParam.contains(AppConfig.KEY_SCENARIO_NAME)) {
				Assert.assertTrue(
					"Information about scenario name must be correct in configuration table",
					confParam.contains(TestConstants.SCENARIO_RAMPUP)
				);
			}
		}
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = LogValidator.getMessageFile(RUN_ID);
		Assert.assertTrue("message.log file must be exist", messageFile.exists());
		//
		try (final BufferedReader
			 bufferedReader = new BufferedReader(new FileReader(messageFile))
		) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TestConstants.SCENARIO_END_INDICATOR)) {
					break;
				}
			}
			Assert.assertNotNull(
				"message.log file doesn't contain information about scenario end",
				line
			);
			Assert.assertTrue(
				"message.log file doesn't contain information about scenario end",
				line.contains(TestConstants.SCENARIO_END_INDICATOR)
			);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file must be exist", perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		// Get data.items.csv file
		final File dataItemFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("data.items.csv file must be exist", dataItemFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectDataItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
	throws Exception {
		// Get perf.trace.csv file
		final File perfTraceFile = LogValidator.getPerfTraceFile(RUN_ID);
		Assert.assertTrue("perf.trace.csv file doesn't exist",perfTraceFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfTraceFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfTraceCSV(in);
		}
	}

	@Test
	public void shouldContainedInformationAboutAllLoadsByStep()
	throws Exception {
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file must be exist", perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			int iterationCount = 4, stepsCount = 0;
			final Set<String> loadsSet = new HashSet<>();
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
				} else if(nextRec.size() == 25) {
					if(iterationCount == 4) {
						iterationCount = 0;
						stepsCount ++;
						//
						Assert.assertTrue(
							"There are not all load types in this step", loadsSet.isEmpty()
						);
						loadsSet.clear();
						loadsSet.add(TestConstants.LOAD_CREATE);
						loadsSet.add(TestConstants.LOAD_READ);
						loadsSet.add(TestConstants.LOAD_UPDATE);
						loadsSet.add(TestConstants.LOAD_APPEND);
						loadsSet.add(TestConstants.LOAD_DELETE);
					} else {
						iterationCount++;
					}
					//
					Assert.assertTrue(
						"This load already exist in this step", loadsSet.contains(nextRec.get(3))
					);
					loadsSet.remove(nextRec.get(3));
					Assert.assertNotEquals(
						"Count of success equals 0 ", 0, nextRec.get(7)
					);
				}
			}
			Assert.assertEquals(
				"Steps counts must be equal to " + COUNT_STEPS, COUNT_STEPS, stepsCount
			);
		}
	}
}
