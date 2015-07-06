package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.integ.integTestTools.ContentGetter;
import com.emc.mongoose.integ.integTestTools.IntegConstants;
import com.emc.mongoose.integ.integTestTools.LogFileManager;
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
import com.emc.mongoose.run.scenario.ScriptRunner;
import com.emc.mongoose.storage.mock.impl.Cinderella;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by olga on 03.07.15.
 */
public final class ReadBackItemsWithSize10ByteDefaultScenarioIntegTest {

	private static SavedOutputStream savedOutputStream;
	//
	private static String
		CREATE_RUN_ID = "create",
		READ_RUN_ID = "read";
	//
	public static final DateFormat FMT_DT = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS", Locale.ROOT) {
		{ setTimeZone(TimeZone.getTimeZone("UTC")); }
	};
	//
	private static final int DATA_COUNT = 10;
	private static final int DATA_SIZE = 10;

	@BeforeClass
	public static void before()
	throws Exception{
		//Create run ID
		CREATE_RUN_ID += ":" + DATA_SIZE + ":" + FMT_DT.format(
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
		//final Logger rootLogger = LogManager.getRootLogger();
		//
		RunTimeConfig.initContext();
		//Run the write default mongoose scenario in standalone mode
		Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, CREATE_RUN_ID);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_STANDALONE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, DATA_COUNT);
				RunTimeConfig.getContext()
					.set(RunTimeConfig.KEY_DATA_SIZE_MAX, String.valueOf(DATA_SIZE) + IntegConstants.BYTE);
				RunTimeConfig.getContext()
					.set(RunTimeConfig.KEY_DATA_SIZE_MIN, String.valueOf(DATA_SIZE) + IntegConstants.BYTE);
				//rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
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

		Thread readScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, READ_RUN_ID);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_STANDALONE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, 0);
				RunTimeConfig.getContext()
					.set(RunTimeConfig.KEY_DATA_SRC_FPATH, LogFileManager.getDataItemsFile(CREATE_RUN_ID).getPath());
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, IntegConstants.LOAD_READ);
				//rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "readScenarioMongoose");
		//
		readScenarioMongoose.start();
		readScenarioMongoose.join();
		readScenarioMongoose.interrupt();
		// Set back System.out stream
		System.setOut(savedOutputStream.getPrintStream());
	}

	@Test
	public void shouldWriteAllDataItemsInCorrectSize()
		throws Exception{
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
			Assert.assertEquals(DATA_SIZE, dataSize);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that all data items are written
		Assert.assertEquals(countDataItems, DATA_COUNT);
	}

	@Test
	public void shouldGetAllWrittenObjectsFromServerAndDataSizeIsCorrect()
		throws Exception{
		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = LogFileManager.getDataItemsFile(CREATE_RUN_ID);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		String line = bufferedReader.readLine(), dataID;
		int actualDataSize;

		while (line != null){
			dataID = line.split(",")[IntegConstants.DATA_ID_COLUMN_INDEX];
			actualDataSize = ContentGetter.getDataSize(dataID);
			Assert.assertEquals(DATA_SIZE, actualDataSize);
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
		throws Exception{
		//Read perf.summary file and search check log's level of summary message
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
		throws Exception{
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SCENARIO_END_INDICATOR));
	}


}
