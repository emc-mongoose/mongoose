package com.emc.mongoose.integ.feature.core;

import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.LogPatterns;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.integ.tools.PortListener;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
//
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by olga on 08.07.15.
 * Covers TC #4(name: "Single write load using several concurrent threads/connections.", steps: all for load.threads=10)
 * in Mongoose Core Functional Testing
 * HLUC: 1.3.2.1
 */
public class WriteUsing10ConnTest
extends WSMockTestBase {
	//
	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final String RUN_ID = WriteUsing10ConnTest.class.getCanonicalName();
	private static final String DATA_SIZE = "0B";
	private static final int LIMIT_TIME = 40, LOAD_CONNS = 10;

	private static Thread SCENARIO_THREAD;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		WSMockTestBase.setUpClass();
		//
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, Integer.toString(LIMIT_TIME));
		appConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		appConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		appConfig.set(RunTimeConfig.KEY_CREATE_CONNS, Integer.toString(LOAD_CONNS));
		appConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		RunTimeConfig.setContext(appConfig);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, BasicConfig.THREAD_CONTEXT.get().toString());
		//  write
		STD_OUTPUT_STREAM = StdOutUtil.getStdOutBufferingStream();
		SCENARIO_THREAD = new Thread(
			new Runnable() {
				@Override
				public void run() {
					new ScenarioRunner().run();
				}
			}, "writeScenarioThread"
		);
		SCENARIO_THREAD.start();
		SCENARIO_THREAD.join(10000);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		try {
			SCENARIO_THREAD.interrupt();
			TimeUnit.SECONDS.sleep(1);
			RunIdFileManager.flushAll();
			//
			Path expectedFile = LogValidator.getMessageFile(RUN_ID).toPath();
			//  Check that messages.log exists
			Assert.assertTrue("messages.log file doesn't exist", Files.exists(expectedFile));
			expectedFile = LogValidator.getPerfAvgFile(RUN_ID).toPath();
			//  Check that perf.avg.csv file exists
			Assert.assertTrue("perf.avg.csv file doesn't exist", Files.exists(expectedFile));
			expectedFile = LogValidator.getPerfTraceFile(RUN_ID).toPath();
			//  Check that perf.trace.csv file exists
			Assert.assertTrue("perf.trace.csv file doesn't exist", Files.exists(expectedFile));
			expectedFile = LogValidator.getItemsListFile(RUN_ID).toPath();
			//  Check that data.items.csv file exists
			Assert.assertTrue("data.items.csv file doesn't exist", Files.exists(expectedFile));
			//
			shouldCreateDataItemsFileWithInformationAboutAllObjects();
			//
			Assert.assertTrue("Console doesn't contain information about summary metrics",
				STD_OUTPUT_STREAM.toString().contains(TestConstants.SUMMARY_INDICATOR)
			);
			Assert.assertTrue("Console doesn't contain information about end of scenario",
				STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
			);
			shouldReportScenarioEndToMessageLogFile();
		} finally {
			STD_OUTPUT_STREAM.close();
			WSMockTestBase.tearDownClass();
		}
	}

	public static void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		//  Read data.items.csv file
		final File dataItemsFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("data.items.csv file doesn't exist", dataItemsFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemsFile.toPath(), StandardCharsets.UTF_8)
		) {
			//
			int countDataItems = 0;
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				Assert.assertEquals(
					"Size of data item isn't correct",
					Long.toString(SizeUtil.toSize(DATA_SIZE)), nextRec.get(2)
				);
				countDataItems++;
			}
			//  Check that there are 10 lines in data.items.csv file
			Assert.assertTrue(
				"Not correct information about created data items", countDataItems > 10
			);
		}
	}

	public static void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//  Read the message file and search for "Scenario end"
		final File messageFile = LogValidator.getMessageFile(RUN_ID);
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
	public void shouldBeActiveAllConnections()
	throws Exception {
		for (int i = 0; i < 3; i++) {
			Thread.sleep(10000);
			int countConnections = PortListener
					.getCountConnectionsOnPort(TestConstants.PORT_INDICATOR);
			// Check that actual connection count = (LOAD_CONNS * 2 + 5) because cinderella is run local
			int actualConnCount = (LOAD_CONNS * 2 + 5);
			Assert.assertTrue("Connection count is wrong", actualConnCount < countConnections && countConnections < actualConnCount + 3); //todo temp changes
		}
	}

	@Test
	public void shouldAllThreadsProduceWorkload()
	throws Exception {
		Matcher matcher;
		String threadName;
		int countProduceWorkloadThreads = 0;
		final Map<Thread, StackTraceElement[]> stackTraceElementMap = Thread.getAllStackTraces();
		for (final Thread thread : stackTraceElementMap.keySet()) {
			threadName = thread.getName();
			matcher = Pattern.compile(
				LogPatterns.CONSOLE_FULL_LOAD_NAME.pattern() + "\\#[\\d]").matcher(threadName
			);
			if (matcher.find()) {
				countProduceWorkloadThreads ++;
			}
		}
		Assert.assertEquals(
			"Wrong count of I/O worker threads", ThreadUtil.getWorkerCount(), countProduceWorkloadThreads
		);
	}

	@Test
	public void shouldCreateCorrectDataItemsFiles()
	throws Exception {
		// Get data.items.csv file of write scenario run
		final File dataItemFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("data.items.csv file doesn't exist", dataItemFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectDataItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFiles()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
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
	public void shouldCreateCorrectInformationAboutLoad()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = LogValidator.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("perfAvg.csv file doesn't exist", perfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			int actualNodesCount, actualConnectionsCount;
			for(final CSVRecord nextRec : recIter) {
				if (firstRow) {
					firstRow = false;
				} else {
					Assert.assertEquals(
						"Storage API is wrong", TestConstants.API_S3, nextRec.get(2).toLowerCase()
					);
					Assert.assertEquals(
						"Type load is wrong",
						BasicConfig.THREAD_CONTEXT.get().getScenarioSingleLoad().toLowerCase(),
						nextRec.get(3).toLowerCase()
					);
					actualConnectionsCount = Integer.valueOf(nextRec.get(4));
					Assert.assertEquals(
						"Count of connections is wrong", LOAD_CONNS, actualConnectionsCount
					);
					actualNodesCount = Integer.valueOf(nextRec.get(5));
					Assert.assertEquals(
						"Count of nodes is wrong", 1, actualNodesCount
					);
				}
			}
		}
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
			final int period = BasicConfig.THREAD_CONTEXT.get().getLoadMetricsPeriodSec();
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
