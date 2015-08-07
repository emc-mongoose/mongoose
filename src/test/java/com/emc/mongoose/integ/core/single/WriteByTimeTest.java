package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.TimeUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * Created by olga on 08.07.15.
 * Covers TC #6(name: "Limit the single write load job w/ both data item count and timeout",
 * steps: all, dominant limit: time) in Mongoose Core Functional Testing
 * HLUC: 1.1.6.2, 1.1.6.4
 */
public class WriteByTimeTest {
	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final String RUN_ID = WriteByTimeTest.class.getCanonicalName();
	private static final String DATA_SIZE = "1B", LIMIT_TIME = "1.minutes";
	private static final int LIMIT_COUNT = 1000000000, LOAD_THREADS = 10;

	private static Logger LOG;

	private static long TIME_ACTUAL_SEC;

	@BeforeClass
	public static void before()
	throws Exception {
		//  remove log dir w/ previous logs
		LogParser.removeLogDirectory(RUN_ID);
		//
		RunTimeConfig.setContext(RunTimeConfig.getDefaultCfg());
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
		rtConfig.set(RunTimeConfig.KEY_LOAD_TYPE_CREATE_THREADS, LOAD_THREADS);
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
			TIME_ACTUAL_SEC = System.currentTimeMillis();
			new ScriptRunner().run();
			TIME_ACTUAL_SEC = System.currentTimeMillis() - TIME_ACTUAL_SEC;
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
	public void shouldRunScenarioFor1Minutes()
	throws Exception {
		final int precisionMillis = 5000;
		//1.minutes = 60000.milliseconds
		final long loadLimitTimeMillis = TimeUtil.getTimeValue(LIMIT_TIME) * 60000;
		Assert.assertTrue(
			(TIME_ACTUAL_SEC >= loadLimitTimeMillis) &&
			(TIME_ACTUAL_SEC <= loadLimitTimeMillis + precisionMillis)
		);
		//
		final File perfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue(perfAvgFile.exists());
		//
		BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));
		//
		final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		Date startTime = null, finishTime = null;
		// Get start time of load
		bufferedReader.readLine();
		String line = bufferedReader.readLine();
		Matcher matcher = TestConstants.TIME_PATTERN.matcher(line);
		if (matcher.find()) {
			startTime = format.parse(matcher.group());
		}
		// Get finish time of load
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue(perfSumFile.exists());
		//
		bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();
		line = bufferedReader.readLine();
		matcher = TestConstants.TIME_PATTERN.matcher(line);
		if (matcher.find()) {
			finishTime = format.parse(matcher.group());
		}
		//
		Assert.assertNotNull(startTime);
		Assert.assertNotNull(finishTime);
		final long differenceTime = finishTime.getTime() - startTime.getTime();
		Assert.assertTrue(
			differenceTime >= loadLimitTimeMillis - precisionMillis &&
			differenceTime < loadLimitTimeMillis + precisionMillis
		);
		bufferedReader.close();
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

		expectedFile = LogParser.getErrorsFile(RUN_ID).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
	}

	@Test
	public void shouldGeneralStatusOfTheRunIsRegularlyReports()
	throws Exception {
		final int precisionMillis = 3000;
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
			String line = bufferedReader.readLine();
			final List<Date> listTimeOfReports = new ArrayList<>();
			while (line != null) {
				matcher = TestConstants.TIME_PATTERN.matcher(line);
				if (matcher.find()) {
					listTimeOfReports.add(format.parse(matcher.group()));
				}
				line = bufferedReader.readLine();
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
				// period must be equal 10 seconds = 10000 milliseconds
				Assert.assertTrue(
					10000 - precisionMillis < (nextTime - firstTime) &&
					10000 + precisionMillis > (nextTime - firstTime)
				);
			}
		}
	}
}
