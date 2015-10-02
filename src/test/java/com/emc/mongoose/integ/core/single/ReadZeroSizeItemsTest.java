package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.base.LoggingTestBase;
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.ContentGetter;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.run.scenario.runner.ScriptMockRunner;
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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 10.07.15.
 * HLUC: 1.1.2.2, 1.1.4.1, 1.1.5.4, 1.3.9.1
 */
public class ReadZeroSizeItemsTest
extends WSMockTestBase {

	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final int LIMIT_COUNT = 10;
	private static final int LIMIT_TIME_SEC = 100;
	private static final String DATA_SIZE = "0B";
	private static final String RUN_ID = ReadZeroSizeItemsTest.class.getCanonicalName();

	private static final String
		CREATE_RUN_ID = RUN_ID + TestConstants.LOAD_CREATE,
		READ_RUN_ID = RUN_ID + TestConstants.LOAD_READ;

	@BeforeClass
	public static void setUpClass()
	throws Exception{
		System.setProperty(RunTimeConfig.KEY_RUN_ID, CREATE_RUN_ID);
		WSMockTestBase.setUpClass();
		//
		RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT));
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		RunTimeConfig.setContext(rtConfig);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//  write
		UniformDataSource.DEFAULT = new UniformDataSource();
		new ScriptMockRunner().run();
		//
		RunIdFileManager.flushAll();
		//
		System.setProperty(RunTimeConfig.KEY_RUN_ID, READ_RUN_ID);
		LoggingTestBase.setUpClass();
		//
		rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_DATA_SRC_FPATH,
			LogValidator.getDataItemsFile(CREATE_RUN_ID).getPath());
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, TestConstants.LOAD_READ);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, Long.toString(LIMIT_TIME_SEC) + "s");
		rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		RunTimeConfig.setContext(rtConfig);
		//
		logger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//  read
		UniformDataSource.DEFAULT = new UniformDataSource();
		try (final BufferingOutputStream
				 stdOutStream = StdOutInterceptorTestSuite.getStdOutBufferingStream()
		) {
			new ScriptMockRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(5);
			STD_OUTPUT_STREAM = stdOutStream;
		}
		//
		RunIdFileManager.flushAll();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(
			"Console doesn't contain information about summary metrics",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SUMMARY_INDICATOR)
		);
		Assert.assertTrue(
			"Console doesn't contain information about end of scenario",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		//  Read data.items.csv file
		final File dataItemsFile = LogValidator.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue(
			"data.items.csv file for create load doesn't exist", dataItemsFile.exists()
		);
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
			Assert.assertEquals(
				"Not correct information about created data items", LIMIT_COUNT, countDataItems
			);
		}
	}

	@Test
	public void shouldGetAllDataItemsFromServerAndDataSizeIsCorrect()
	throws Exception {
		//  Read data.items.csv file
		final File dataItemsFile = LogValidator.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue("data.items.csv file doesn't exist", dataItemsFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemsFile.toPath(), StandardCharsets.UTF_8)
		) {
			int actualDataSize;
			//
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				try {
					actualDataSize = ContentGetter.getDataSize(nextRec.get(0), TestConstants.BUCKET_NAME);
					Assert.assertEquals(
						"Size of data item isn't correct", SizeUtil.toSize(DATA_SIZE), actualDataSize
					);
				} catch (final IOException e) {
					Assert.fail(String.format("Failed to get data item %s from server", nextRec.get(0)));
				}
			}
		}
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//  Read message file and search "Scenario End"
		final File messageFile = LogValidator.getMessageFile(READ_RUN_ID);
		Assert.assertTrue(messageFile.exists());
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
	public void shouldCreateAllFilesWithLogsAfterWriteScenario()
	throws Exception {
		Path expectedFile = LogValidator.getMessageFile(CREATE_RUN_ID).toPath();
		//  Check that messages.log exists
		Assert.assertTrue("messages.log file of create load doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfAvgFile(CREATE_RUN_ID).toPath();
		//  Check that perf.avg.csv file exists
		Assert.assertTrue("perf.avg.csv file of create load doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfSumFile(CREATE_RUN_ID).toPath();
		//  Check that perf.sum.csv file exists
		Assert.assertTrue("perf.sum.csv file of create load doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfTraceFile(CREATE_RUN_ID).toPath();
		//  Check that perf.trace.csv file exists
		Assert.assertTrue("perf.trace.csv file of create load doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getDataItemsFile(CREATE_RUN_ID).toPath();
		//  Check that data.items.csv file exists
		Assert.assertTrue("data.items.csv file of create load doesn't exist", Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateAllFilesWithLogsAfterReadScenario()
	throws Exception {
		Path expectedFile = LogValidator.getMessageFile(READ_RUN_ID).toPath();
		//  Check that messages.log file is contained
		Assert.assertTrue("messages.log file of read load doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfAvgFile(READ_RUN_ID).toPath();
		//  Check that perf.avg.csv file is contained
		Assert.assertTrue("perf.avg.csv file of read load doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfSumFile(READ_RUN_ID).toPath();
		//  Check that perf.sum.csv file is contained
		Assert.assertTrue("perf.sum.csv file of read load doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfTraceFile(READ_RUN_ID).toPath();
		//  Check that perf.trace.csv file is contained
		Assert.assertTrue("perf.trace.csv file of read load doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getDataItemsFile(READ_RUN_ID).toPath();
		//  Check that data.items.csv file is contained
		Assert.assertTrue("data.items.csv file of read load doesn't exist", Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateCorrectDataItemsFileAfterReadScenario()
	throws Exception {
		//  Get data.items.csv file of read scenario run
		final File readDataItemFile = LogValidator.getDataItemsFile(READ_RUN_ID);
		Assert.assertTrue("data.items.csv file doesn't exist", readDataItemFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(readDataItemFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectDataItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFileAfterReadScenario()
	throws Exception {
		// Get perf.sum.csv file of read scenario run
		final File readPerfSumFile = LogValidator.getPerfSumFile(READ_RUN_ID);
		Assert.assertTrue("perf.sum.csv file doesn't exist", readPerfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(readPerfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFileAfterReadScenario()
	throws Exception {
		// Get perf.avg.csv file
		final File readPerfAvgFile = LogValidator.getPerfAvgFile(READ_RUN_ID);
		Assert.assertTrue("perfAvg.csv file doesn't exist", readPerfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(readPerfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfAvgCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFileAfterReadScenario()
	throws Exception {
		// Get perf.trace.csv file
		final File readPerfTraceFile = LogValidator.getPerfTraceFile(READ_RUN_ID);
		Assert.assertTrue("perf.trace.csv file doesn't exist", readPerfTraceFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(readPerfTraceFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfTraceCSV(in);
		}
	}

	@Test
	public void shouldWriteAllDataItemsInCorrectSize()
	throws Exception {
		//  Read data.items.csv file
		final File dataItemsFile = LogValidator.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue("data.items.csv file of create load doesn't exist", dataItemsFile.exists());
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
			Assert.assertEquals(
				"Not correct information about created data items", LIMIT_COUNT, countDataItems
			);
		}
	}

	@Test
	public void shouldReportCorrectCountOfReadObjectToSummaryLogFile()
	throws Exception {
		//  Read perf.summary file
		final File perfSumFile = LogValidator.getPerfSumFile(READ_RUN_ID);

		//  Check that file exists
		Assert.assertTrue("perf.sum.csv file of read load doesn't exist", perfSumFile.exists());

		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			//
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if (firstRow) {
					firstRow = false;
				} else if (nextRec.size() == 23) {
					Assert.assertTrue(
						"Count of success is not integer", LogValidator.isInteger(nextRec.get(7))
					);
					Assert.assertEquals(
						"Count of success isn't correct", Integer.toString(LIMIT_COUNT), nextRec.get(7)
					);
				}
			}
		}
	}

	@Test
	public void shouldReadDataItemsInSameOrderAsInFileOfWriteScenario()
	throws Exception {
		//  Get data.items.csv file of create run
		final File dataItemsFileWrite = LogValidator.getDataItemsFile(CREATE_RUN_ID);
		Assert.assertTrue("data.items.csv file of create load doesn't exist", dataItemsFileWrite.exists());
		//
		final byte[] bytesDataItemsFileWrite = Files.readAllBytes(dataItemsFileWrite.toPath());
		//  Get data.items.csv file of read run
		final File dataItemsFileRead = LogValidator.getDataItemsFile(READ_RUN_ID);
		Assert.assertTrue("data.items.csv file of read load doesn't exist", dataItemsFileRead.exists());
		//
		final byte[] bytesDataItemsFileRead = Files.readAllBytes(dataItemsFileRead.toPath());
		//  Check files are equal
		Assert.assertTrue(
			"File data.items.csv of create load and data.items.csv file of read load aren't equals",
			Arrays.equals(bytesDataItemsFileRead, bytesDataItemsFileWrite));
	}
}
