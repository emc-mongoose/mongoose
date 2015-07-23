package com.emc.mongoose.integ.core.rampup;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.LogPatterns;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;

/**
 * Created by olga on 22.07.15.
 * HLUC: 1.5.3.1
 */
public class DefaultRampupTest {
	//
	private static BufferingOutputStream savedOutputStream;
	//
	private static String rampupRunID;
	private static final String	LIMIT_TIME = "30.seconds";
	private static final int COUNT_STEPS = 70;

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

		expectedFile = LogParser.getErrorsFile(rampupRunID).toPath();
		Assert.assertFalse("errors.log file must not be contained", Files.exists(expectedFile));
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
		// Get perf.sum.csv file of write scenario run
		final File perfSumFile = LogParser.getPerfSumFile(rampupRunID);
		Assert.assertTrue("perf.sum.csv file must be exist", perfSumFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(
			String.format("Header %s of perf.sum.csv file must be correct", line),
			LogParser.HEADER_PERF_SUM_FILE, line
		);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(
				String.format("Line %s must be correct", line),
				LogParser.matchWithPerfSumFilePattern(line)
			);
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectDataItemsFile()
		throws Exception {
		// Get data.items.csv file of write scenario run
		final File dataItemFile = LogParser.getDataItemsFile(rampupRunID);
		Assert.assertTrue("data.items.csv file must be exist", dataItemFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemFile));
		//
		String line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(
				String.format("Line %s must be correct", line),
				LogParser.matchWithDataItemsFilePattern(line)
			);
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
		throws Exception {
		// Get perf.trace.csv file of write scenario run
		final File perfTraceFile = LogParser.getPerfTraceFile(rampupRunID);
		Assert.assertTrue("perf.trace.csv file must be exist", perfTraceFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfTraceFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(
			String.format("Header %s of perf.sum.csv file must be correct", line),
			LogParser.HEADER_PERF_TRACE_FILE, line
		);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(
				String.format("Line %s must be correct", line),
				LogParser.matchWithPerfTraceFilePattern(line)
			);
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldContainedInformationAboutAllLoadsByStep()
		throws Exception {
		final File perfSumFile = LogParser.getPerfSumFile(rampupRunID);
		Assert.assertTrue("perf.sum.csv file must be exist", perfSumFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();
		//
		int countSteps = 0, countSucc;
		final int zeroVal = 0;
		String line;
		do {
			line = bufferedReader.readLine();
			if (line == null) {
				break;
			}
			countSteps++;
			final Set<String> loadsSet = new HashSet<>();
			loadsSet.add(TestConstants.LOAD_CREATE);
			loadsSet.add(TestConstants.LOAD_READ);
			loadsSet.add(TestConstants.LOAD_UPDATE);
			loadsSet.add(TestConstants.LOAD_APPEND);
			loadsSet.add(TestConstants.LOAD_DELETE);
			line = bufferedReader.readLine();
			for (int i = 0; i < 5 && line != null; i++) {
				Matcher matcher = LogPatterns.PERF_SUM_FILE.matcher(line);
				if (matcher.find()) {
					// Geet load type from step
					if ( loadsSet.contains(matcher.group("typeLoad")) ) {
						loadsSet.remove(matcher.group("typeLoad"));
					}
					// Get Count Succ from load
					if ( loadsSet.contains(matcher.group("countSucc")) ) {
						countSucc = Integer.valueOf(matcher.group("countSucc"));
						Assert.assertNotEquals(
							String.format("Value of metric: %d - must be bigger than 0", countSucc),
							zeroVal, countSucc
						);
					}
				}
				line = bufferedReader.readLine();
			}
			Assert.assertTrue(
				String.format("Step doesn't contain %d load type in step %d", loadsSet.size(), countSteps),
				loadsSet.isEmpty()
			);
			line = bufferedReader.readLine();
		} while (line != null);
		Assert.assertEquals("Steps counts must be equal" + COUNT_STEPS, COUNT_STEPS, countSteps);
	}
}