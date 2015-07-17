package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.integTestTools.IntegConstants;
import com.emc.mongoose.integ.integTestTools.IntegLogManager;
import com.emc.mongoose.integ.integTestTools.PortListener;
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;

/**
 * Created by olga on 08.07.15.
 * Covers TC #4(name: "Single write load using several concurrent threads/connections.", steps: all for load.threads=100)
 * in Mongoose Core Functional Testing
 * HLUC: 1.3.2.2
 */
public class SingleWriteScenarioWith100ConcurrentConnectionsIntegTest {
	//
	private static SavedOutputStream savedOutputStream;
	//
	private static final int LIMIT_COUNT = 1000000, LOAD_THREADS = 100;
	private static String createRunId = IntegConstants.LOAD_CREATE;
	private static final String DATA_SIZE = "0B";
	private static Thread writeScenarioMongoose;

	@BeforeClass
	public static void before()
	throws Exception {
		// Set new saved console output stream
		savedOutputStream = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		//Create run ID
		createRunId += ":" + DATA_SIZE + ":" + IntegConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, createRunId);
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(IntegConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, IntegConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(IntegConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = org.apache.logging.log4j.LogManager.getRootLogger();
		//Reload default properties
		final RunTimeConfig runTimeConfig = new RunTimeConfig();
		runTimeConfig.loadProperties();
		RunTimeConfig.setContext(runTimeConfig);
		//run mongoose default scenario in standalone mode
		writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, createRunId);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
				RunTimeConfig.getContext().set("load.type.create.threads", LOAD_THREADS);
				// For correct work of verification option
				UniformDataSource.DEFAULT = new UniformDataSource();
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		/*
		// Before start Mongoose
		int countConnections = PortListener.getCountConnectionsOnPort(IntegConstants.PORT_INDICATOR);
		// Check that actual connection count = 1 because cinderella is run local (for single test run)
		Assert.assertEquals(1, countConnections);
		*/
		// Start Mongoose
		writeScenarioMongoose.start();
		writeScenarioMongoose.join(30000);
	}
	@AfterClass
	public static void after()
	throws Exception {
		if (!writeScenarioMongoose.isInterrupted()) {
			writeScenarioMongoose.join();
			writeScenarioMongoose.interrupt();
		}
		// Wait logger's output from console
		Thread.sleep(3000);
		//
		Path expectedFile = IntegLogManager.getMessageFile(createRunId).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getPerfAvgFile(createRunId).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getPerfTraceFile(createRunId).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getDataItemsFile(createRunId).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getErrorsFile(createRunId).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
		//
		shouldCreateDataItemsFileWithInformationAboutAllObjects();

		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SCENARIO_END_INDICATOR));
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SUMMARY_INDICATOR));

		shouldReportScenarioEndToMessageLogFile();

		System.setOut(savedOutputStream.getPrintStream());
	}

	public static void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = IntegLogManager.getDataItemsFile(createRunId);
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		int dataSize, countDataItems = 0;
		String line = bufferedReader.readLine();

		while (line != null){
			// Get dataSize from each line
			dataSize = Integer.valueOf(line.split(",")[IntegConstants.DATA_SIZE_COLUMN_INDEX]);
			Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), dataSize);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that there are 10 lines in data.items.csv file
		Assert.assertEquals(LIMIT_COUNT, countDataItems);
	}

	public static void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = IntegLogManager.getMessageFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile));
		// Search line in file which contains "Scenario end" string.
		// Get out from the loop when line with "Scenario end" if found else returned line = null
		String line;
		do {
			line = bufferedReader.readLine();
		} while ((!line.contains(IntegConstants.SCENARIO_END_INDICATOR)) && line != null);

		//Check the message file contain report about scenario end. If not line = null.
		Assert.assertTrue(line.contains(IntegConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldBeActiveAllConnections()
	throws Exception {
		for (int i = 0; i < 3; i++) {
			int countConnections = PortListener.getCountConnectionsOnPort(IntegConstants.PORT_INDICATOR);
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
			matcher = IntegConstants.LOAD_THRED_NAME_PATTERN.matcher(threadName);
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
		final File dataItemFile = IntegLogManager.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemFile));
		//
		String line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(IntegLogManager.matchWithDataItemsFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFiles()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = IntegLogManager.getPerfAvgFile(createRunId);
		Assert.assertTrue(perfAvgFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(IntegLogManager.HEADER_PERF_AVG_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(IntegLogManager.matchWithPerfAvgFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectInformationAboutLoad()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = IntegLogManager.getPerfAvgFile(createRunId);
		Assert.assertTrue(perfAvgFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(IntegLogManager.HEADER_PERF_AVG_FILE, line);
		//
		Matcher matcher;
		String loadType, actualLoadType, apiName;
		String[] loadInfo;
		int threadsPerNode, countNode;
		//
		line = bufferedReader.readLine();
		while (line != null) {
			//
			matcher = IntegConstants.LOAD_PATTERN.matcher(line);
			if (matcher.find()) {
				loadInfo = matcher.group().split("(-|x)");
				//Check api name is correct
				apiName = loadInfo[1].toLowerCase();
				Assert.assertEquals(IntegConstants.API_S3, apiName);
				// Check load type and load limit count values are correct
				loadType = RunTimeConfig.getContext().getScenarioSingleLoad().toLowerCase() + String.valueOf(LIMIT_COUNT);
				actualLoadType = loadInfo[2].toLowerCase();
				Assert.assertEquals(loadType, actualLoadType);
				// Check "threads per node" value is correct
				threadsPerNode = Integer.valueOf(loadInfo[3]);
				Assert.assertEquals(LOAD_THREADS, threadsPerNode);
				//Check node count is correct
				countNode = Integer.valueOf(loadInfo[4]);
				Assert.assertEquals( 1 , countNode);
			}
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldGeneralStatusOfTheRunIsRegularlyReports()
	throws Exception {
		final int precisionMillis = 1000;
		// Get perf.avg.csv file
		final File perfAvgFile = IntegLogManager.getPerfAvgFile(createRunId);
		Assert.assertTrue(perfAvgFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));

		final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		Matcher matcher;
		//
		bufferedReader.readLine();
		String line = bufferedReader.readLine();
		final List<Date> listTimeOfReports = new ArrayList<>();
		while (line != null) {
			matcher = IntegConstants.TIME_PATTERN.matcher(line);
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
