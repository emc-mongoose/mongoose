package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
//
import com.emc.mongoose.run.scenario.ScriptRunner;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

/**
 * Created by olga on 07.07.15.
 * Covers TC #2(name: "Read back the data items written in the different run.", steps: 3 for data.size=10MB)
 * in Mongoose Core Functional Testing
 * HLUC: 1.1.4.2
 */
public class ReadVerificationTest {
	private static BufferingOutputStream savedOutputStream;
	//
	private static String
		createRunId = TestConstants.LOAD_CREATE,
		readRunId = TestConstants.LOAD_READ;
	//
	private static final int LIMIT_COUNT = 10;
	private static final String DATA_SIZE = "10B";

	@BeforeClass
	public static void before()
	throws Exception{
		//Create run ID
		createRunId += ":" + DATA_SIZE + ":" + TestConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, createRunId);
		// Init logger and runtime config
		final String fullLogConfFile = Paths
			.get(
				System.getProperty(TestConstants.USER_DIR_PROPERTY_NAME),
				Constants.DIR_CONF, TestConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(TestConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = LogManager.getRootLogger();
		//Reload default properties
		RunTimeConfig runTimeConfig = new RunTimeConfig();
		runTimeConfig.loadProperties();
		RunTimeConfig.setContext(runTimeConfig);
		//Run the write default mongoose scenario in standalone mode
		final Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, createRunId);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
				// For correct work of verification option
				UniformDataSource.DEFAULT = new UniformDataSource();
				//
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		writeScenarioMongoose.start();
		writeScenarioMongoose.join();
		writeScenarioMongoose.interrupt();

		savedOutputStream = new BufferingOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		//Create new run ID
		readRunId += ":" + DATA_SIZE + ":" + LogUtil.FMT_DT.format(
			Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, readRunId);
		//Reload default properties
		runTimeConfig = new RunTimeConfig();
		runTimeConfig.loadProperties();
		RunTimeConfig.setContext(runTimeConfig);
		//
		final Thread readScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, readRunId);
				RunTimeConfig.getContext()
					.set(RunTimeConfig.KEY_DATA_SRC_FPATH, LogParser.getDataItemsFile(createRunId).getPath());
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, TestConstants.LOAD_READ);
				//Set another seed -> it has to brake verification
				final String newSeed = "7a42d9c483244166";
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_RING_SEED, newSeed);
				System.setProperty(RunTimeConfig.KEY_DATA_RING_SEED, newSeed);
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				// For correct work of verification option
				UniformDataSource.DEFAULT = new UniformDataSource();
				new ScriptRunner().run();
			}
		}, "readScenarioMongoose");
		//
		readScenarioMongoose.start();
		readScenarioMongoose.join();
		readScenarioMongoose.interrupt();
		Thread.sleep(3000);
		//
		System.setOut(savedOutputStream.getReplacedStream());
	}

	@Test
	public void shouldFailedReadOfAllDataItems()
	throws Exception {
		// Get perf.sum.csv file of read scenario
		final File perfSumFile = LogParser.getPerfSumFile(readRunId);
		Assert.assertTrue(perfSumFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();
		int countFail = Integer.valueOf(bufferedReader.readLine().split(",")[TestConstants.COUNT_FAIL_COLUMN_INDEX]);
		Assert.assertEquals(LIMIT_COUNT, countFail);
	}

	@Test
	public void shouldReportAboutFailedVerificationToConsole()
	throws Exception {
		// Get data.items.csv file of write scenario
		final File dataItemsFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemsFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));
		String line = bufferedReader.readLine();
		String dataID;
		while (line != null){
			dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
			Assert.assertTrue(savedOutputStream.toString().contains(dataID+ TestConstants.CONTENT_MISMATCH_INDICATOR));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldReportAboutFailedVerificationToMessageFile()
	throws Exception {
		// Get data.items.csv file of write scenario
		final File dataItemsFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemsFile.exists());
		//
		final BufferedReader bufferedDataItemsReader = new BufferedReader(new FileReader(dataItemsFile));
		// Get content of message.log file of read scenario
		final String contentMessageFile = new Scanner(LogParser.getMessageFile(readRunId))
			.useDelimiter("\\Z")
			.next();
		String line = bufferedDataItemsReader.readLine();
		String dataID;
		while (line != null){
			dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
			Assert.assertTrue(contentMessageFile.contains(dataID + TestConstants.CONTENT_MISMATCH_INDICATOR));
			line = bufferedDataItemsReader.readLine();
		}
	}
}
