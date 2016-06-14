package com.emc.mongoose.system.feature.limit;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.system.base.ScenarioTestBase;
import com.emc.mongoose.system.tools.StdOutUtil;
import com.emc.mongoose.system.tools.LogPatterns;
import com.emc.mongoose.system.tools.TestConstants;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.system.tools.BufferingOutputStream;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.emc.mongoose.common.conf.SizeInBytes.toFixedSize;

/**
 * Created by olga on 08.07.15.
 * Covers TC #6(name: "Limit the single write load job w/ both data item count and timeout",
 * steps: all, dominant limit: time) in Mongoose Core Functional Testing
 * HLUC: 1.1.6.2, 1.1.6.4
 */
public class WriteBySizeTest
extends ScenarioTestBase {

	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final String RUN_ID = WriteBySizeTest.class.getCanonicalName();
	private static final String DATA_SIZE = "64KB", LIMIT_SIZE = "4GB";
	private static final int LIMIT_COUNT = 0, LOAD_THREADS = 10;
	private final static long EXPECTED_COUNT = toFixedSize(LIMIT_SIZE) / toFixedSize(DATA_SIZE);
	//
	private static long TIME_ACTUAL_SEC;

	@BeforeClass
	public static void setUpClass() {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		ScenarioTestBase.setUpClass();
		//
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT));
		appConfig.setProperty(AppConfig.KEY_ITEM_DATA_SIZE, DATA_SIZE);
		appConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_SIZE, LIMIT_SIZE);
		appConfig.setProperty(AppConfig.KEY_LOAD_THREADS, Integer.toString(LOAD_THREADS));
		appConfig.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, RUN_ID);
		appConfig.setProperty(AppConfig.KEY_LOAD_METRICS_PERIOD, 10);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, BasicConfig.THREAD_CONTEXT.get().toString());
		//
		try(
			final BufferingOutputStream
				stdOutStream =	StdOutUtil.getStdOutBufferingStream()
		) {
			STD_OUTPUT_STREAM = stdOutStream;
			TIME_ACTUAL_SEC = System.currentTimeMillis();
			SCENARIO_RUNNER.run();
			TIME_ACTUAL_SEC = System.currentTimeMillis() - TIME_ACTUAL_SEC;
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(10);
		} catch(final IOException | InterruptedException e) {

		}
		//
		try {
			RunIdFileManager.flushAll();
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
	}

	@AfterClass
	public  static void tearDownClass() {
		ScenarioTestBase.tearDownClass();
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsToConsole()
	throws Exception {
		Assert.assertTrue("Console doesn't contain information about summary metrics",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SUMMARY_INDICATOR)
		);
		Assert.assertTrue("Console doesn't contain information about end of scenario",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//  Read the message file and search for "Scenario end"
		final File messageFile = LogValidator.getMessageLogFile(RUN_ID);
		Assert.assertTrue(
			"messages.log file doesn't exist",
			messageFile.exists()
		);
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(messageFile))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TestConstants.SCENARIO_END_INDICATOR)) {
					break;
				}
			}
			Assert.assertNotNull(
				"Line with information about end of scenario must not be equal null ", line
			);
			Assert.assertTrue(
				"Information about end of scenario doesn't contain in message.log file",
				line.contains(TestConstants.SCENARIO_END_INDICATOR)
			);
		}
	}

	@Test
	public void checkWrittenCount()
	throws Exception {
		final File itemsFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("data.items.csv file doesn't exist", itemsFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(itemsFile.toPath(), StandardCharsets.UTF_8)
		) {
			//
			int countDataItems = 0;
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				Assert.assertEquals(
					"Size of data item isn't correct",
					Long.toString(toFixedSize(DATA_SIZE)), nextRec.get(2)
				);
				countDataItems ++;
			}
			//  Check that there are 10 lines in data.items.csv file
			Assert.assertEquals(
				"The written count differs from the expected", EXPECTED_COUNT, countDataItems
			);
		}
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
		//  Check that data.items.csv file exists
		Assert.assertTrue("data.items.csv file doesn't exist", Files.exists(expectedFile));
	}

	@Test
	public void shouldGeneralStatusOfTheRunIsRegularlyReports()
	throws Exception {
		final int precisionMillis = 3000;
		// Get perf.avg.csv file
		final File perfAvgFile = LogValidator.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("perf.avg.csv file doesn't exist", perfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			Matcher matcher;
			final List<Date> listTimeOfReports = new ArrayList<>();
			final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
			//
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for (final CSVRecord nextRec : recIter) {
				if (firstRow) {
					firstRow = false;
				} else {
					matcher = LogPatterns.DATE_TIME_ISO8601.matcher(nextRec.get(0));
					if (matcher.find()) {
						listTimeOfReports.add(format.parse(matcher.group("time")));
					} else {
						Assert.fail("Data and time record in line has got wrong format");
					}
				}
			}
			// Check period of reports is correct
			long firstTime, nextTime;
			// Period must be equal 10 sec
			final int period = BasicConfig.THREAD_CONTEXT.get().getLoadMetricsPeriod();
			// period must be equal 10 seconds = 10000 milliseconds
			Assert.assertEquals("Wrong load.metrics.periodSec in configuration", 10, period);

			for (int i = 0; i < listTimeOfReports.size() - 1; i++) {
				firstTime = listTimeOfReports.get(i).getTime();
				nextTime = listTimeOfReports.get(i + 1).getTime();
				// period must be equal 10 seconds = 10000 milliseconds
				Assert.assertEquals(
					"Load metrics period is wrong", 10000, nextTime - firstTime, precisionMillis
				);
			}
		}
	}
}
