package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.ContentGetter;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 07.07.15.
 * Covers TC #2(name: "Read back the data items written in the different run.", steps: 1-2 for data.size=200MB)
 * in Mongoose Core Functional Testing
 * HLUC: 1.1.2.6, 1.1.4.5, 1.1.5.4, 1.3.9.1
 */
public class Read200MBItemsTest {

	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final int LIMIT_COUNT = 10;
	private static final String DATA_SIZE = "200MB";
	private static final String RUN_ID = Read200MBItemsTest.class.getCanonicalName();

	private static final String
		CREATE_RUN_ID = RUN_ID + TestConstants.LOAD_CREATE,
		READ_RUN_ID = RUN_ID + TestConstants.LOAD_READ;

	private static Logger LOG;

	@BeforeClass
	public static void before()
	throws Exception{
		//  remove log dir w/ previous logs
		LogParser.removeLogDirectory(CREATE_RUN_ID);
		LogParser.removeLogDirectory(READ_RUN_ID);
		//
		RunTimeConfig.setContext(RunTimeConfig.getDefaultCfg());
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, CREATE_RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		LoggingTestSuite.setUpClass();

		LOG = LogManager.getLogger();
		//  write
		executeLoadJob(rtConfig);
		//  reset before using
		StdOutInterceptorTestSuite.reset();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, READ_RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_DATA_SRC_FPATH, LogParser
			.getDataItemsFile(CREATE_RUN_ID).getPath());
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, TestConstants.LOAD_READ);
		//  read
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
			new ScriptRunner().run();
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
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		//  Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			int dataSize, countDataItems = 0;
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				//  Get dataSize from each line
				dataSize = Integer.valueOf(line.split(",")[TestConstants.DATA_SIZE_COLUMN_INDEX]);
				Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), dataSize);
				countDataItems++;
			}
			//  Check that there are 10 lines in data.items.csv file
			Assert.assertEquals(LIMIT_COUNT, countDataItems);
		}
	}

	@Test
	public void shouldGetAllObjectsFromServerAndDataSizeIsDefault()
	throws Exception {
		//  Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			String line, dataID;
			int actualDataSize;

			while ((line = bufferedReader.readLine()) != null) {
				dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
				actualDataSize = ContentGetter.getDataSize(dataID);
				Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), actualDataSize);
			}
		}
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = LogParser.getMessageFile(READ_RUN_ID);
		Assert.assertTrue(messageFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(messageFile))) {
			// Search line in file which contains "Scenario end" string.
			// Get out from the loop when line with "Scenario end" if found else returned line = null
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
	public void shouldCreateAllFilesWithLogsAfterWriteScenario()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(CREATE_RUN_ID).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(CREATE_RUN_ID).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(CREATE_RUN_ID).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(CREATE_RUN_ID).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(CREATE_RUN_ID).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getErrorsFile(CREATE_RUN_ID).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateAllFilesWithLogsAfterReadScenario()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(READ_RUN_ID).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(READ_RUN_ID).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(READ_RUN_ID).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(READ_RUN_ID).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(READ_RUN_ID).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getErrorsFile(READ_RUN_ID).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateCorrectDataItemsFileAfterReadScenario()
	throws Exception {
		// Get data.items.csv file of read scenario run
		final File readDataItemFile = LogParser.getDataItemsFile(READ_RUN_ID);
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
		final File readPerfSumFile = LogParser.getPerfSumFile(READ_RUN_ID);
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
		final File readPerfAvgFile = LogParser.getPerfAvgFile(READ_RUN_ID);
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
		final File readPerfTraceFile = LogParser.getPerfTraceFile(READ_RUN_ID);
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
		final File writeDataItemFile = LogParser.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue(writeDataItemFile.exists());

		//Check correct data size in data.items.csv file
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(writeDataItemFile))) {
			String line;
			String[] dataItemsColumns,layerAndMaskColumn;
			int dataSize, countDataItems = 0;
			while ((line = bufferedReader.readLine()) != null) {
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
			}
			//Check that all data items are written
			Assert.assertEquals(countDataItems, LIMIT_COUNT);
		}
	}

	@Test
	public void shouldGetAllWrittenObjectsFromServerAndDataSizeIsCorrect()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			String line, dataID;
			int actualDataSize;

			while ((line = bufferedReader.readLine()) != null) {
				dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
				actualDataSize = ContentGetter.getDataSize(dataID);
				Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), actualDataSize);
			}
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		//Read perf.summary file of read scenario run
		final File perfSumFile = LogParser.getPerfSumFile(READ_RUN_ID);

		//Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(perfSumFile))) {
			//  read header of csv file
			bufferedReader.readLine();
			// Get value of "CountSucc" column
			final int actualCountSucc = Integer.valueOf(
				bufferedReader.readLine().split(",")[TestConstants.COUNT_SUCC_COLUMN_INDEX]
			);
			Assert.assertEquals(actualCountSucc, LIMIT_COUNT);
		}
	}
}
