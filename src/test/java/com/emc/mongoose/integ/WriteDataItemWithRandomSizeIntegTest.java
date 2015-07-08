package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.integ.integTestTools.IntegConstants;
import com.emc.mongoose.integ.integTestTools.LogFileManager;
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
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
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by olga on 08.07.15.
 * Covers TC #3 (name: "Write the data items w/ random size.", steps: all) in Mongoose Core Functional Testing
 */
public class WriteDataItemWithRandomSizeIntegTest {
	//
	private static SavedOutputStream savedOutputStream;
	//
	private static final int DATA_COUNT = 10;
	private static String createRunId = IntegConstants.LOAD_CREATE;
	private static final String
		DATA_SIZE_MIN = "10B",
		DATA_SIZE_MAX = "100KB";

	@BeforeClass
	public static void before()
	throws Exception {
		// Set new saved console output stream
		savedOutputStream = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		// Create run ID
		createRunId += ":" + DATA_SIZE_MIN + "-" + DATA_SIZE_MAX + ":" + IntegConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, createRunId);
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(IntegConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, IntegConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(IntegConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = LogManager.getRootLogger();
		// Reload default properties
		RunTimeConfig runTimeConfig = new  RunTimeConfig();
		RunTimeConfig.setContext(runTimeConfig);
		// Run mongoose default scenario in standalone mode
		Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, createRunId);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, DATA_COUNT);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE_MIN);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE_MAX);
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		writeScenarioMongoose.start();
		writeScenarioMongoose.join();
		writeScenarioMongoose.interrupt();
	}

	@AfterClass
	public static void after()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SCENARIO_END_INDICATOR));
		System.setOut(savedOutputStream.getPrintStream());
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SUMMARY_INDICATOR));
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogFileManager.getMessageFile(createRunId).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getPerfAvgFile(createRunId).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getPerfSumFile(createRunId).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getPerfTraceFile(createRunId).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getDataItemsFile(createRunId).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getErrorsFile(createRunId).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		// Read perf.summary file of create scenario run
		final File perfSumFile = LogFileManager.getPerfSumFile(createRunId);

		// Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();

		// Get value of "CountSucc" column
		final int actualCountSucc = Integer.valueOf(
			bufferedReader.readLine().split(",")[IntegConstants.COUNT_SUCC_COLUMN_INDEX]
		);
		Assert.assertEquals(DATA_COUNT, actualCountSucc);
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		// Read data.items.csv file of create scenario run
		final File dataItemsFile = LogFileManager.getDataItemsFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		int dataSize, countDataItems = 0, actualMinSize = Integer.MAX_VALUE, actualMaxSize = 0;
		String line = bufferedReader.readLine();
		// Size of this set must be > 1. It's mean that there are different data items sizes
		final Set<Integer> dataItemSizes = new HashSet<>();

		//
		while (line != null) {
			// Get dataSize from each line
			dataSize = Integer.valueOf(line.split(",")[IntegConstants.DATA_SIZE_COLUMN_INDEX]);
			dataItemSizes.add(dataSize);
			if (dataSize < actualMinSize) {
				actualMinSize = dataSize;
			} else if (dataSize > actualMaxSize) {
				actualMaxSize = dataSize;
			}
			countDataItems++;
			line = bufferedReader.readLine();
		}
		// Check that there are 10 lines in data.items.csv file
		Assert.assertEquals(DATA_COUNT, countDataItems);
		// Check that there are different data sizes
		Assert.assertTrue(dataItemSizes.size() > 1);
		// Check data items min size is correct
		Assert.assertTrue(SizeUtil.toSize(DATA_SIZE_MIN) <= actualMinSize);
		// Check data items max size is correct
		Assert.assertTrue(SizeUtil.toSize(DATA_SIZE_MAX) >= actualMaxSize);
	}

	@Test
	public void shouldCreateCorrectDataItemsFilesAfterWriteScenario()
	throws Exception {
		// Get data.items.csv file of write scenario run
		final File writeDataItemFile = LogFileManager.getDataItemsFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(writeDataItemFile));
		//
		String line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogFileManager.matchWithDataItemsFilePattern(line));
			line = bufferedReader.readLine();
		}
	}
}
