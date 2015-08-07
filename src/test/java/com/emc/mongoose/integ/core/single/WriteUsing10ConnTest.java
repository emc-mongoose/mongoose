package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.PortListener;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
//
import com.emc.mongoose.run.scenario.ScriptRunner;
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

/**
 * Created by olga on 08.07.15.
 * Covers TC #4(name: "Single write load using several concurrent threads/connections.", steps: all for load.threads=10)
 * in Mongoose Core Functional Testing
 * HLUC: 1.3.2.1
 */
public class WriteUsing10ConnTest {
	//
	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final String RUN_ID = WriteUsing10ConnTest.class.getCanonicalName();
	private static final String DATA_SIZE = "0B";
	private static final int LIMIT_COUNT = 1000000, LOAD_THREADS = 10;

	private static Logger LOG;

	private static Thread SCENARIO_THREAD;

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
		rtConfig.set(RunTimeConfig.KEY_LOAD_TYPE_CREATE_THREADS, LOAD_THREADS);
		LoggingTestSuite.setUpClass();

		LOG = LogManager.getLogger();
		//  write
		SCENARIO_THREAD = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					executeLoadJob(rtConfig);
				} catch (final Exception e) {
					Assert.fail("Failed to execute load job");
				}
			}
		}, "writeScenarioThread");
		SCENARIO_THREAD.start();
		SCENARIO_THREAD.join(30000);
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

	@AfterClass
	public static void after()
	throws Exception {
		if (!SCENARIO_THREAD.isInterrupted()) {
			SCENARIO_THREAD.join();
			SCENARIO_THREAD.interrupt();
		}
		TimeUnit.SECONDS.sleep(3);
		//
		Path expectedFile = LogParser.getMessageFile(RUN_ID).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(RUN_ID).toPath();
		//Check that perf.avg.csv file is contained
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
		//
		shouldCreateDataItemsFileWithInformationAboutAllObjects();
		//
		Assert.assertTrue(STD_OUTPUT_STREAM.toString()
			.contains(TestConstants.SCENARIO_END_INDICATOR));
		Assert.assertTrue(STD_OUTPUT_STREAM.toString()
			.contains(TestConstants.SUMMARY_INDICATOR));

		shouldReportScenarioEndToMessageLogFile();

		STD_OUTPUT_STREAM.close();

	}

	public static void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			int dataSize, countDataItems = 0;
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				// Get dataSize from each line
				dataSize = Integer.valueOf(line.split(",")[TestConstants.DATA_SIZE_COLUMN_INDEX]);
				Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), dataSize);
				countDataItems++;
			}
			//Check that there are 10 lines in data.items.csv file
			Assert.assertEquals(LIMIT_COUNT, countDataItems);
		}
	}

	public static void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = LogParser.getMessageFile(RUN_ID);
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
	public void shouldBeActiveAllConnections()
	throws Exception {
		for (int i = 0; i < 3; i++) {
			int countConnections = PortListener
					.getCountConnectionsOnPort(TestConstants.PORT_INDICATOR);
			// Check that actual connection count = (LOAD_THREADS * 2 + 1) because cinderella is run local
			Assert.assertEquals((LOAD_THREADS * 2 + 1), countConnections);
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
			matcher = TestConstants.LOAD_THRED_NAME_PATTERN.matcher(threadName);
			if (matcher.find()) {
				countProduceWorkloadThreads++;
			}
		}
		Assert.assertEquals(LOAD_THREADS, countProduceWorkloadThreads);
	}

	@Test
	public void shouldCreateCorrectDataItemsFiles()
	throws Exception {
		// Get data.items.csv file of write scenario run
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
	public void shouldCreateCorrectPerfAvgFiles()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
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
	public void shouldCreateCorrectInformationAboutLoad()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue(perfAvgFile.exists());
		//
		try (final BufferedReader bufferedReader =
		        new BufferedReader(new FileReader(perfAvgFile))) {
			String line;
			Matcher matcher;
			String loadType, actualLoadType, apiName;
			String[] loadInfo;
			int threadsPerNode, countNode;
			//  read header of csv file
			bufferedReader.readLine();
			while ((line = bufferedReader.readLine()) != null) {
				//
				matcher = TestConstants.LOAD_PATTERN.matcher(line);
				if (matcher.find()) {
					loadInfo = matcher.group().split("(-|x)");
					//Check api name is correct
					apiName = loadInfo[1].toLowerCase();
					Assert.assertEquals(TestConstants.API_S3, apiName);
					// Check load type and load limit count values are correct
					loadType = RunTimeConfig.getContext().getScenarioSingleLoad()
						.toLowerCase() + String.valueOf(LIMIT_COUNT);
					actualLoadType = loadInfo[2].toLowerCase();
					Assert.assertEquals(loadType, actualLoadType);
					// Check "threads per node" value is correct
					threadsPerNode = Integer.valueOf(loadInfo[3]);
					Assert.assertEquals(LOAD_THREADS, threadsPerNode);
					//Check node count is correct
					countNode = Integer.valueOf(loadInfo[4]);
					Assert.assertEquals(1, countNode);
				}
			}
		}
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
			//  read header of csv file
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
				// period must be equal 10 seconds = 10000 milliseconds
				Assert.assertTrue(
					10000 - precisionMillis < (nextTime - firstTime) &&
					10000 + precisionMillis > (nextTime - firstTime)
				);
			}
		}
		//
	}
}
