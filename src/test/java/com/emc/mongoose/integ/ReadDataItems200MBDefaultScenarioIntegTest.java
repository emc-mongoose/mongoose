package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.integ.integTestTools.ContentGetter;
import com.emc.mongoose.integ.integTestTools.IntegConstants;
import com.emc.mongoose.integ.integTestTools.LogFileManager;
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
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
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by olga on 07.07.15.
 * Covers TC #2(name: "Read back the data items written in the different run.", steps: 1-2 for data.size=200MB)
 * in Mongoose Core Functional Testing
 */
public class ReadDataItems200MBDefaultScenarioIntegTest {

	private static SavedOutputStream savedOutputStream;
	//
	private static String
		CREATE_RUN_ID = IntegConstants.LOAD_CREATE,
		READ_RUN_ID = IntegConstants.LOAD_READ;
	//
	private static final int DATA_COUNT = 10;
	private static final String DATA_SIZE = "200MB";

	@BeforeClass
	public static void before()
	throws Exception{
		//Create run ID
		CREATE_RUN_ID += ":" + DATA_SIZE + ":" + IntegConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID,CREATE_RUN_ID);
		// Init logger and runtime config
		final String fullLogConfFile = Paths
			.get(
				System.getProperty(IntegConstants.USER_DIR_PROPERTY_NAME),
				Constants.DIR_CONF, IntegConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(IntegConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = LogManager.getRootLogger();
		//Reload default properties
		RunTimeConfig runTimeConfig = new  RunTimeConfig();
		RunTimeConfig.setContext(runTimeConfig);
		//Run the write default mongoose scenario in standalone mode
		Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, CREATE_RUN_ID);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, DATA_COUNT);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		writeScenarioMongoose.start();
		writeScenarioMongoose.join();
		writeScenarioMongoose.interrupt();

		savedOutputStream = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		//Create new run ID
		READ_RUN_ID += ":" + DATA_SIZE + ":" + LogUtil.FMT_DT.format(
			Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, READ_RUN_ID);
		//Reload default properties
		runTimeConfig = new  RunTimeConfig();
		RunTimeConfig.setContext(runTimeConfig);
		//
		Thread readScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, READ_RUN_ID);
				RunTimeConfig.getContext()
					.set(RunTimeConfig.KEY_DATA_SRC_FPATH, LogFileManager.getDataItemsFile(CREATE_RUN_ID).getPath());
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, IntegConstants.LOAD_READ);
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "readScenarioMongoose");
		//
		readScenarioMongoose.start();
		readScenarioMongoose.join();
		readScenarioMongoose.interrupt();
	}

	@Test
	public void shouldCreateCorrectDataItemsFilesAfterReadScenario()
	throws Exception {
		// Get data.items.csv file of read scenario run
		final File readDataItemFile = LogFileManager.getDataItemsFile(READ_RUN_ID);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(readDataItemFile));
		//
		String line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogFileManager.matchWithDataItemsFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFilesAfterReadScenario()
	throws Exception {
		// Get perf.sum.csv file of read scenario run
		final File readPerfSumFile = LogFileManager.getPerfSumFile(READ_RUN_ID);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(readPerfSumFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(LogFileManager.HEADER_PERF_SUM_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogFileManager.matchWithPerfSumFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFilesAfterReadScenario()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File readPerfAvgFile = LogFileManager.getPerfAvgFile(READ_RUN_ID);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(readPerfAvgFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(LogFileManager.HEADER_PERF_AVG_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogFileManager.matchWithPerfAvgFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFilesAfterReadScenario()
	throws Exception {
		// Get perf.trace.csv file of write scenario run
		final File readPerfTraceFile = LogFileManager.getPerfTraceFile(READ_RUN_ID);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(readPerfTraceFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(LogFileManager.HEADER_PERF_TRACE_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogFileManager.matchWithPerfTraceFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldWriteAllDataItemsInCorrectSize()
	throws Exception {
		// Get data.items.csv file of write scenario run
		final File writeDataItemFile = LogFileManager.getDataItemsFile(CREATE_RUN_ID);
		//Check correct data size in data.items.csv file
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(writeDataItemFile));
		String line = bufferedReader.readLine();
		String[] dataItemsColumns,layerAndMaskColumn;
		int dataSize, countDataItems = 0;
		while (line != null) {
			dataItemsColumns = line.split(",");
			//Check that all columns are contained in data.items.csv
			Assert.assertEquals(dataItemsColumns.length, IntegConstants.DATA_ITEMS_COLUMN_COUNT);
			//Check that last column contain layer number and mask number (2 elements)
			layerAndMaskColumn = dataItemsColumns[dataItemsColumns.length - 1].split("/");
			Assert.assertEquals(layerAndMaskColumn.length, 2);
			//Check that data items have correct data size
			dataSize = Integer.valueOf(dataItemsColumns[IntegConstants.DATA_SIZE_COLUMN_INDEX]);
			Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), dataSize);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that all data items are written
		Assert.assertEquals(countDataItems, DATA_COUNT);
	}

	@Test
	public void shouldGetAllWrittenObjectsFromServerAndDataSizeIsCorrect()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = LogFileManager.getDataItemsFile(CREATE_RUN_ID);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		String line = bufferedReader.readLine(), dataID;
		int actualDataSize;

		while (line != null){
			dataID = line.split(",")[IntegConstants.DATA_ID_COLUMN_INDEX];
			actualDataSize = ContentGetter.getDataSize(dataID);
			Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), actualDataSize);
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		//Read perf.summary file of read scenario run
		final File perfSumFile = LogFileManager.getPerfSumFile(READ_RUN_ID);

		//Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();

		// Get value of "CountSucc" column
		final int actualCountSucc = Integer.valueOf(
			bufferedReader.readLine().split(",")[IntegConstants.COUNT_SUCC_COLUMN_INDEX]
		);
		Assert.assertEquals(actualCountSucc, DATA_COUNT);
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SCENARIO_END_INDICATOR));
	}
}
