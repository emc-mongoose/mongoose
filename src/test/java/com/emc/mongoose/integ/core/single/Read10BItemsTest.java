package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.tools.ContentGetter;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
//
import com.emc.mongoose.run.scenario.ScriptRunner;
//
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by olga on 03.07.15.
 * Covers TC #2(name: "Read back the data items written in the different run.", steps: 1-2 for data.size=10B)
 * in Mongoose Core Functional Testing
 * HLUC: 1.1.2.3, 1.1.4.2, 1.1.5.4, 1.3.9.1
 */
public final class Read10BItemsTest {

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
		final Logger rootLogger = org.apache.logging.log4j.LogManager.getRootLogger();
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
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "readScenarioMongoose");
		//
		readScenarioMongoose.start();
		readScenarioMongoose.join();
		readScenarioMongoose.interrupt();
		// Wait logger's output from console
		Thread.sleep(3000);
		savedOutputStream.close();
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(TestConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(savedOutputStream.toString().contains(TestConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemsFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		int dataSize, countDataItems = 0;
		String line = bufferedReader.readLine();

		while (line != null){
			// Get dataSize from each line
			dataSize = Integer.valueOf(line.split(",")[TestConstants.DATA_SIZE_COLUMN_INDEX]);
			Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), dataSize);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that there are 10 lines in data.items.csv file
		Assert.assertEquals(LIMIT_COUNT, countDataItems);
	}


	@Test
	public void shouldGetAllObjectsFromServerAndDataSizeIsDefault()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemsFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		String line = bufferedReader.readLine(), dataID;
		int actualDataSize;

		while (line != null){
			dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
			actualDataSize = ContentGetter.getDataSize(dataID);
			Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), actualDataSize);
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = LogParser.getMessageFile(readRunId);
		Assert.assertTrue(messageFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile));
		// Search line in file which contains "Scenario end" string.
		// Get out from the loop when line with "Scenario end" if found else returned line = null
		String line;
		do {
			line = bufferedReader.readLine();
		} while ((!line.contains(TestConstants.SCENARIO_END_INDICATOR)) && line != null);

		//Check the message file contain report about scenario end. If not line = null.
		Assert.assertTrue(line.contains(TestConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldCreateAllFilesWithLogsAfterWriteScenario()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(createRunId).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(createRunId).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(createRunId).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(createRunId).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(createRunId).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getErrorsFile(createRunId).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateAllFilesWithLogsAfterReadScenario()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(readRunId).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(readRunId).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(readRunId).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(readRunId).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(readRunId).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getErrorsFile(readRunId).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateCorrectDataItemsFileAfterReadScenario()
	throws Exception {
		// Get data.items.csv file of read scenario run
		final File readDataItemFile = LogParser.getDataItemsFile(readRunId);
		Assert.assertTrue(readDataItemFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(readDataItemFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectDataItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFileAfterReadScenario()
	throws Exception {
		// Get perf.sum.csv file of read scenario run
		final File readPerfSumFile = LogParser.getPerfSumFile(readRunId);
		Assert.assertTrue(readPerfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(readPerfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFileAfterReadScenario()
	throws Exception {
		// Get perf.avg.csv file
		final File readPerfAvgFile = LogParser.getPerfAvgFile(readRunId);
		Assert.assertTrue("perfAvg.csv file doesn't exist", readPerfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(readPerfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfAvgCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFileAfterReadScenario()
	throws Exception {
		// Get perf.trace.csv file
		final File readPerfTraceFile = LogParser.getPerfTraceFile(readRunId);
		Assert.assertTrue("perf.trace.csv file doesn't exist",readPerfTraceFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(readPerfTraceFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfTraceCSV(in);
		}
	}

	@Test
	public void shouldWriteAllDataItemsInCorrectSize()
	throws Exception {
		// Get data.items.csv file of write scenario run
		final File writeDataItemFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(writeDataItemFile.exists());

		//Check correct data size in data.items.csv file
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(writeDataItemFile));
		String line = bufferedReader.readLine();
		String[] dataItemsColumns,layerAndMaskColumn;
		int dataSize, countDataItems = 0;
		while (line != null) {
			dataItemsColumns = line.split(",");
			//Check that all columns are contained in data.items.csv
			Assert.assertEquals(dataItemsColumns.length, TestConstants.DATA_ITEMS_COLUMN_COUNT);
			//Check that last column contain layer number and mask number (2 elements)
			layerAndMaskColumn = dataItemsColumns[dataItemsColumns.length - 1].split("/");
			Assert.assertEquals(layerAndMaskColumn.length, 2);
			//Check that data items have correct data size
			dataSize = Integer.valueOf(dataItemsColumns[TestConstants.DATA_SIZE_COLUMN_INDEX]);
			Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), dataSize);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that all data items are written
		Assert.assertEquals(countDataItems, LIMIT_COUNT);
	}

	@Test
	public void shouldGetAllWrittenObjectsFromServerAndDataSizeIsCorrect()
	throws Exception {
		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemsFile.exists());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		String line = bufferedReader.readLine(), dataID;
		int actualDataSize;

		while (line != null){
			dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
			actualDataSize = ContentGetter.getDataSize(dataID);
			Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), actualDataSize);
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		//Read perf.summary file of read single scenario
		final File perfSumFile = LogParser.getPerfSumFile(readRunId);
		//Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();

		// Get value of "CountSucc" column
		final int actualCountSucc = Integer.valueOf(
			bufferedReader.readLine().split(",")[TestConstants.COUNT_SUCC_COLUMN_INDEX]
		);
		Assert.assertEquals(actualCountSucc, LIMIT_COUNT);
	}

	@Test
	public void shouldReadDataItemsInSameOrderAsInFileOfWriteScenario()
	throws Exception {
		// Get data.items.csv file of create run
		final File dataItemsFileWrite = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemsFileWrite.exists());
		//
		final byte[] bytesDataItemsFileWrite = Files.readAllBytes(dataItemsFileWrite.toPath());
		// Get data.items.csv file of read run
		final File dataItemsFileRead = LogParser.getDataItemsFile(readRunId);
		Assert.assertTrue(dataItemsFileRead.exists());
		//
		final byte[] bytesDataItemsFileRead = Files.readAllBytes(dataItemsFileRead.toPath());
		// Check files are equal
		Assert.assertTrue(Arrays.equals(bytesDataItemsFileRead, bytesDataItemsFileWrite));
	}

}
