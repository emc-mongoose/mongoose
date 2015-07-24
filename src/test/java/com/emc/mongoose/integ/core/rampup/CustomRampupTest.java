package com.emc.mongoose.integ.core.rampup;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by olga on 22.07.15.
 * HLUC: 1.4.4.1, 1.5.3.3, 1.5.4.9, 1.5.6.5
 */
public class CustomRampupTest {
	//
	private static BufferingOutputStream savedOutputStream;
	//
	private static String rampupRunID;
	private static final String
		LIMIT_TIME = "60.seconds",
		RAMPUP_SIZES = "10KB,1MB,10MB",
		RAMPUP_THREAD_COUNTS = "10,50,100";
	private static final int COUNT_STEPS = 9;

	@BeforeClass
	public static void before()
		throws Exception {
		// Set new saved console output stream
		savedOutputStream = new BufferingOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		//Create run ID
		rampupRunID = TestConstants.SCENARIO_RAMPUP + ":" + TestConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, rampupRunID);
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(TestConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, TestConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(TestConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = org.apache.logging.log4j.LogManager.getRootLogger();
		//Reload default properties
		final RunTimeConfig runTimeConfig = new RunTimeConfig();
		runTimeConfig.loadProperties();
		RunTimeConfig.setContext(runTimeConfig);
		//run mongoose default scenario in standalone mode
		final Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, rampupRunID);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_NAME, TestConstants.SCENARIO_RAMPUP);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_RAMPUP_SIZES, RAMPUP_SIZES);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_RAMPUP_THREAD_COUNTS, RAMPUP_THREAD_COUNTS);
				// For correct work of verification option
				UniformDataSource.DEFAULT = new UniformDataSource();
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		writeScenarioMongoose.start();
		writeScenarioMongoose.join();
		writeScenarioMongoose.interrupt();
		// Wait logger's output from console
		Thread.sleep(3000);
		savedOutputStream.close();
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
		throws Exception {
		Assert.assertTrue(
			"Should report information about end of scenario run from console",
			savedOutputStream.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
		throws Exception {
		Path expectedFile = LogParser.getMessageFile(rampupRunID).toPath();
		Assert.assertTrue("messages.log file must be contained", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(rampupRunID).toPath();
		Assert.assertTrue("perf.sum.csv file must be contained", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(rampupRunID).toPath();
		Assert.assertTrue("perf.trace.csv file must be contained", Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(rampupRunID).toPath();
		Assert.assertTrue("data.items.csv file must be contained", Files.exists(expectedFile));

		/*
		expectedFile = LogParser.getErrorsFile(rampupRunID).toPath();
		Assert.assertFalse("errors.log file must not be contained", Files.exists(expectedFile));
		*/
	}

	@Test
	public void shouldCustomValuesDisplayedCorrectlyInConfigurationTable()
		throws Exception {
		final String[] runtimeConfCustomParam = RunTimeConfig.getContext().toString().split("\n");
		for (final String confParam : runtimeConfCustomParam) {
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(
					"Information about time limit must be correct in configuration table",
					confParam.contains(LIMIT_TIME)
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(
					"Information about storage address must be correct in configuration table",
					confParam.contains("127.0.0.1")
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(
					"Information about run mode must be correct in configuration table",
					confParam.contains(Constants.RUN_MODE_STANDALONE)
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_ID)) {
				Assert.assertTrue(
					"Information about run ID must be correct in configuration table",
					confParam.contains(rampupRunID)
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_COUNT)) {
				Assert.assertTrue(
					"Information about load limit by count must be correct in configuration table",
					confParam.contains("0")
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_SCENARIO_NAME)) {
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
		final File messageFile = LogParser.getMessageFile(rampupRunID);
		Assert.assertTrue("message.log file must be exist", messageFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile));
		// Search line in file which contains "Scenario end" string.
		// Get out from the loop when line with "Scenario end" if found else returned line = null
		String line;
		do {
			line = bufferedReader.readLine();
		} while ((!line.contains(TestConstants.SCENARIO_END_INDICATOR)) && line != null);

		//Check the message file contain report about scenario end. If not line = null.
		Assert.assertTrue(
			"message.log file must contains information about scenario end",
			line.contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
		throws Exception {
		// Get perf.sum.csv file
		final File perfSumFile = LogParser.getPerfSumFile(rampupRunID);
		Assert.assertTrue("perf.sum.csv file must be exist", perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectDataItemsFile()
		throws Exception {
		// Get data.items.csv file
		final File dataItemFile = LogParser.getDataItemsFile(rampupRunID);
		Assert.assertTrue("data.items.csv file must be exist", dataItemFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectDataItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
		throws Exception {
		// Get perf.trace.csv file
		final File perfTraceFile = LogParser.getPerfTraceFile(rampupRunID);
		Assert.assertTrue("perf.trace.csv file doesn't exist",perfTraceFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfTraceFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfTraceCSV(in);
		}
	}

	@Test
	public void shouldContainedInformationAboutAllLoadsByStep()
		throws Exception {
		final File perfSumFile = LogParser.getPerfSumFile(rampupRunID);
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

				if (firstRow) {
					firstRow = false;
				} else if (nextRec.size() == 21){
					if (iterationCount == 4) {
						iterationCount = 0;
						stepsCount++;
						//
						Assert.assertTrue("There are not all load types in this step", loadsSet.isEmpty());
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
					System.out.println(nextRec.get(3) + " " + loadsSet.size());
					Assert.assertTrue("This load isn't exist in this step", loadsSet.contains(nextRec.get(3)));
					loadsSet.remove(nextRec.get(3));
					Assert.assertNotEquals("Count of success equals 0 ", 0, nextRec.get(7));
				}
			}
			Assert.assertEquals("Steps counts must be equal" + COUNT_STEPS, COUNT_STEPS, stepsCount);
		}
	}
}