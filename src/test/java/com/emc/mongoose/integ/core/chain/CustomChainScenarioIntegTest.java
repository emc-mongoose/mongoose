package com.emc.mongoose.integ.core.chain;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.logging.log4j.LogManager;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * Created by olga on 10.07.15.
 * Covers TC #9 (name: "Custom load chain", steps: all)
 * HLUC: 1.4.2.1, 1.5.3.3
 */
public class CustomChainScenarioIntegTest {
	//
	private static BufferingOutputStream STD_OUTPUT_STREAM;
	//
	private static String RUN_ID = CustomChainScenarioIntegTest.class.getCanonicalName();
	private static final String
		DATA_SIZE = "10KB",
		LIMIT_TIME = "1.minutes",
		SCENARIO_NAME = "chain";
	private static final int LOAD_THREADS = 10;
	private static final boolean VERIFY_CONTENT = false;
	private static final int LOADS_COUNT = 5;

	private static Logger LOG;

	@BeforeClass
	public static void before()
	throws Exception {
		//  remove log dir w/ previous logs
		LogParser.removeLogDirectory(RUN_ID);
		//
		RunTimeConfig.setContext(RunTimeConfig.getDefaultCfg());
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_NAME, SCENARIO_NAME);
		rtConfig.set(TestConstants.KEY_VERIFY_CONTENT, VERIFY_CONTENT);
		rtConfig.set(RunTimeConfig.KEY_LOAD_TYPE_CREATE_THREADS, LOAD_THREADS);
		rtConfig.set(RunTimeConfig.KEY_LOAD_TYPE_READ_THREADS, LOAD_THREADS);
		rtConfig.set(RunTimeConfig.KEY_LOAD_TYPE_UPDATE_THREADS, LOAD_THREADS);
		rtConfig.set(RunTimeConfig.KEY_LOAD_TYPE_DELETE_THREADS, LOAD_THREADS);
		rtConfig.set(RunTimeConfig.KEY_LOAD_TYPE_APPEND_THREADS, LOAD_THREADS);
		LoggingTestSuite.setUpClass();

		LOG = LogManager.getLogger();
		//  write
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
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(STD_OUTPUT_STREAM.toString()
			.contains(TestConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(STD_OUTPUT_STREAM.toString()
			.contains(TestConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = LogParser.getMessageFile(RUN_ID);
		Assert.assertTrue(messageFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(messageFile))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TestConstants.SCENARIO_END_INDICATOR)) {
					break;
				}
			}
			Assert.assertNotNull(line);
			//Check the message file contain report about scenario end. If not line = null.
			Assert.assertTrue(line.contains(TestConstants.SCENARIO_END_INDICATOR));
		}
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
				Assert.assertTrue(confParam.contains("0"));
			}
			if (confParam.contains(RunTimeConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(confParam.contains("127.0.0.1"));
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(confParam.contains(Constants.RUN_MODE_STANDALONE));
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_ID)) {
				Assert.assertTrue(confParam.contains(RUN_ID));
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(confParam.contains(LIMIT_TIME));
			}
			if (confParam.contains(RunTimeConfig.KEY_SCENARIO_NAME)) {
				Assert.assertTrue(confParam.contains(SCENARIO_NAME));
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_THREADS)) {
				Assert.assertTrue(confParam.contains(String.valueOf(LOAD_THREADS)));
			}
		}
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(RUN_ID).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(RUN_ID).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(RUN_ID).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(RUN_ID).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(RUN_ID).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		/*
		expectedFile = IntegLogManager.getErrorsFile(RUN_ID).toPath();
		//Check that errors.lod file isn't contained
		Assert.assertFalse(Files.exists(expectedFile));
		 */
	}

	@Test
	public void shouldCreateCorrectDataItemsFiles()
	throws Exception {
		// Get data.items.csv file
		final File dataItemFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemFile.exists());
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
		final File perfTraceFile = LogParser.getPerfTraceFile(RUN_ID);
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
	public void shouldCreateCorrectPerfAvgFile()
	throws Exception {
		// Get perf.avg.csv file
		final File perfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("perfAvg.csv file doesn't exist", perfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfAvgCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue(perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldLoadsSwitchOperationsAsynchronously()
	throws Exception {
		final File perfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue(perfAvgFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(perfAvgFile))) {
			//
			Matcher matcher;
			int counterSwitch = 1;
			String
				line,
				prevLoadType = TestConstants.LOAD_CREATE;
			while ((line = bufferedReader.readLine()) != null) {
				matcher = TestConstants.LOAD_NAME_PATTERN.matcher(line);
				if (matcher.find()){
					if (!prevLoadType.equals(matcher.group())) {
						counterSwitch++;
						prevLoadType = matcher.group();
					}
				}
			}
			Assert.assertTrue(counterSwitch > LOADS_COUNT);
		}
	}

	@Test
	public void shouldContainedInformationAboutAllLoads()
	throws Exception {
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue(perfSumFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(perfSumFile))) {
			//  read header of csv
			bufferedReader.readLine();
			int countLinesOfSumInfo = 0;
			final Set<String> loadsSet = new HashSet<>();
			loadsSet.add(TestConstants.LOAD_CREATE);
			loadsSet.add(TestConstants.LOAD_READ);
			loadsSet.add(TestConstants.LOAD_UPDATE);
			loadsSet.add(TestConstants.LOAD_APPEND);
			loadsSet.add(TestConstants.LOAD_DELETE);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TestConstants.LOAD_CREATE)
						&& loadsSet.contains(TestConstants.LOAD_CREATE)) {
					loadsSet.remove(TestConstants.LOAD_CREATE);
				}
				if (line.contains(TestConstants.LOAD_READ)
						&& loadsSet.contains(TestConstants.LOAD_READ)) {
					loadsSet.remove(TestConstants.LOAD_READ);
				}
				if (line.contains(TestConstants.LOAD_UPDATE)
						&& loadsSet.contains(TestConstants.LOAD_UPDATE)) {
					loadsSet.remove(TestConstants.LOAD_UPDATE);
				}
				if (line.contains(TestConstants.LOAD_APPEND)
						&& loadsSet.contains(TestConstants.LOAD_APPEND)) {
					loadsSet.remove(TestConstants.LOAD_APPEND);
				}
				if (line.contains(TestConstants.LOAD_DELETE)
						&& loadsSet.contains(TestConstants.LOAD_DELETE)) {
					loadsSet.remove(TestConstants.LOAD_DELETE);
				}
				countLinesOfSumInfo ++;
			}
			Assert.assertTrue(
				"The following load job summaries was not met: " + loadsSet.toString(),
				loadsSet.isEmpty()
			);
			Assert.assertEquals(LOADS_COUNT, countLinesOfSumInfo);
		}
	}

	@Test
	public void shouldEachLoadMustRunFor60Seconds()
	throws Exception {
		final File perfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue(perfAvgFile.exists());
		//
		BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));
		//
		final List<Date>
			startTimeLoad = new ArrayList<>(5),
			finishTimeLoad = new ArrayList<>(5);
		final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		Matcher matcher;
		// Get start time of loads
		bufferedReader.readLine();
		String line = bufferedReader.readLine();
		for (int i = 0; i < 5; i++) {
			matcher = TestConstants.TIME_PATTERN.matcher(line);
			if (matcher.find()) {
				startTimeLoad.add(format.parse(matcher.group()));
			}
		}
		// Get finish time of loads
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue(perfSumFile.exists());
		//
		bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();
		line = bufferedReader.readLine();
		for (int i = 0; i < 5; i++) {
			matcher = TestConstants.TIME_PATTERN.matcher(line);
			if (matcher.find()) {
				finishTimeLoad.add(format.parse(matcher.group()));
			}
		}
		//
		long differenceTime;
		// 1.minutes = 60000.milliseconds
		final int precisionMillis = 3000, loadLimitTimeMillis = 60000;

		for (int i = 0; i < 5; i++) {
			differenceTime = finishTimeLoad.get(i).getTime() - startTimeLoad.get(i).getTime();
			Assert.assertTrue(
				differenceTime > loadLimitTimeMillis - precisionMillis &&
				differenceTime < loadLimitTimeMillis + precisionMillis
			);
		}
		bufferedReader.close();
	}

	@Test
	public void shouldGeneralStatusOfTheRunIsRegularlyReports()
	throws Exception {
		final int precisionMillis = 10000;
		// Get perf.avg.csv file
		final File perfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue(perfAvgFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(perfAvgFile))) {
			final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
			Matcher matcher;
			//
			bufferedReader.readLine();
			String line;
			final List<Date> listTimeOfReports = new ArrayList<>();
			while ((line = bufferedReader.readLine()) != null) {
				matcher = TestConstants.TIME_PATTERN.matcher(line);
				if (matcher.find()) {
					listTimeOfReports.add(format.parse(matcher.group()));
				}
			}
			// Check period of reports is correct
			long firstTime, nextTime;
			// Period must be equal 10 sec
			final int period = RunTimeConfig.getContext().getLoadMetricsPeriodSec();
			// period must be equal 10 seconds = 10000 milliseconds
			Assert.assertEquals(10, period);
			//
			for (int i = 0; i < listTimeOfReports.size() - 1; i++) {
				firstTime = listTimeOfReports.get(i).getTime();
				nextTime = listTimeOfReports.get(i + 1).getTime();
				if (firstTime != nextTime) {
					Assert.assertTrue(
						10000 - precisionMillis < (nextTime - firstTime) &&
						10000 + precisionMillis > (nextTime - firstTime)
					);
				}
			}
		}
	}

	@Test
	public void shouldCreateCorrectInformationAboutLoad()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue(perfAvgFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(perfAvgFile))) {
			bufferedReader.readLine();
			Matcher matcher, loadTypeMatcher;
			String actualLoadType, apiName;
			String[] loadInfo;
			int threadsPerNode, countNode;
			//
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				//
				matcher = TestConstants.LOAD_PATTERN.matcher(line);
				if (matcher.find()) {
					loadInfo = matcher.group().split("(-|x)");
					//Check api name is correct
					apiName = loadInfo[1].toLowerCase();
					Assert.assertEquals(TestConstants.API_S3, apiName);
					// Check load type and load limit count values are correct
					actualLoadType = loadInfo[2];
					loadTypeMatcher = TestConstants.LOAD_NAME_PATTERN.matcher(actualLoadType);
					Assert.assertTrue(loadTypeMatcher.find());
					// Check "threads per node" value is correct
					threadsPerNode = Integer.valueOf(loadInfo[3]);
					Assert.assertEquals(LOAD_THREADS , threadsPerNode);
					//Check node count is correct
					countNode = Integer.valueOf(loadInfo[4]);
					Assert.assertEquals(1, countNode);
				}
			}
		}
	}

	@Test
	public void shouldDataItemsMasksAreUpdate()
	throws Exception {
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			final int firstMaskVal = 0;
			int maskVal;
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				maskVal = Integer.valueOf(line.split("(/)")[1]);
				// Check that data items masks are update and not equal 0
				Assert.assertTrue(maskVal != firstMaskVal);
			}
		}
	}
}
