package com.emc.mongoose.integ.feature.core;

import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 09.07.15.
 * Covers TC #6(name: "Limit the single write load job w/ both data item count and timeout",
 * steps: all, dominant limit: count) in Mongoose Core Functional Testing
 * HLUC: 1.1.5.2, 1.1.5.5
 */
public class WriteByCountTest
extends WSMockTestBase {

	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final String RUN_ID = WriteByCountTest.class.getCanonicalName();
	private static final String DATA_SIZE = "1B", LIMIT_TIME = "365.days";
	private static final int LIMIT_COUNT = 10000, LOAD_THREADS = 10;
	private static final TreeSet<String>
		UNIQ_ITEMS = new TreeSet<>(),
		UNIQ_TRACES = new TreeSet<>();
	private static final List<String>
		DUP_ITEMS = new LinkedList<>(),
		DUP_TRACES = new LinkedList<>(),
		MISSING_ITEMS = new LinkedList<>();

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		WSMockTestBase.setUpClass();
		//
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT));
		appConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		appConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		appConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
		appConfig.set(RunTimeConfig.KEY_CREATE_CONNS, Integer.toString(LOAD_THREADS));
		appConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		RunTimeConfig.setContext(appConfig);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, BasicConfig.THREAD_CONTEXT.get().toString());
		//
		try (final BufferingOutputStream
				 stdOutStream =	StdOutUtil.getStdOutBufferingStream()
		) {
			//  Run mongoose default scenario in standalone mode
			new ScenarioRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(5);
			STD_OUTPUT_STREAM = stdOutStream;
		}
		//
		RunIdFileManager.flushAll();
		TimeUnit.SECONDS.sleep(5);
		//
		String nextLine, values[];
		try(
			final BufferedReader in = Files.newBufferedReader(
				LogValidator.getItemsListFile(RUN_ID).toPath(), StandardCharsets.UTF_8
			)
		) {
			LOG.info(
				Markers.MSG, "Find the duplicates in the \"{}\" file...",
				LogValidator.getItemsListFile(RUN_ID)
			);
			while((nextLine = in.readLine()) != null) {
				values = nextLine.split(",");
				Assert.assertEquals(values.length, 4);
				if(!UNIQ_ITEMS.add(values[0])) {
					DUP_ITEMS.add(values[0]);
				}
			}
		}
		//
		try(
			final BufferedReader in = Files.newBufferedReader(
				LogValidator.getPerfTraceFile(RUN_ID).toPath(), StandardCharsets.UTF_8
			)
		) {
			LOG.info(
				Markers.MSG, "Find the duplicates in the \"{}\" file...",
				LogValidator.getPerfTraceFile(RUN_ID)
			);
			boolean headerLine = true;
			while((nextLine = in.readLine()) != null) {
				if(headerLine) {
					headerLine = false;
					continue;
				}
				values = nextLine.split(",");
				Assert.assertEquals(values.length, 9);
				if(!UNIQ_TRACES.add(values[2])) {
					DUP_TRACES.add(values[2]);
				}
			}
		}
		//
		LOG.info(
			Markers.MSG, "Find the missing items...",
			LogValidator.getItemsListFile(RUN_ID)
		);
		boolean found;
		for(final String oidFromItemTraces : UNIQ_TRACES) {
			found = false;
			for(final String oidFromItemList : UNIQ_ITEMS) {
				if(oidFromItemTraces.equals(oidFromItemList)) {
					found = true;
					break;
				}
			}
			if(!found) {
				MISSING_ITEMS.add(oidFromItemTraces);
			}
		}
		LOG.info(Markers.MSG, "Log analysis done");
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsToConsole()
	throws Exception {
		Assert.assertTrue("Console doesn't contain information about summary metrics",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SUMMARY_INDICATOR)
		);
		Assert.assertTrue("Console doesn't contain information about end of scenario",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
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
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogValidator.getMessageFile(RUN_ID).toPath();
		//  Check that messages.log exists
		Assert.assertTrue("messages.log file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfAvgFile(RUN_ID).toPath();
		//  Check that perf.avg.csv file exists
		Assert.assertTrue("perf.avg.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfSumFile(RUN_ID).toPath();
		//  Check that perf.sum.csv file exists
		Assert.assertTrue("perf.sum.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getPerfTraceFile(RUN_ID).toPath();
		//  Check that perf.trace.csv file exists
		Assert.assertTrue("perf.trace.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogValidator.getItemsListFile(RUN_ID).toPath();
		//  Check that data.items.csv file exists
		Assert.assertTrue("data.items.csv file doesn't exist", Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
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
					Long.toString(SizeInBytes.toFixedSize(DATA_SIZE)), nextRec.get(2)
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
	public void shouldCreateCorrectDataItemsFile()
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
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file of write scenario run
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file doesn't exist", perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		//  Read perf.summary file
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);

		//  Check that file exists
		Assert.assertTrue("perf.sum.csv file doesn't exist", perfSumFile.exists());

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
	public void checkNoDuplicateItemsLogged()
	throws Exception {
		Assert.assertTrue(
			DUP_ITEMS.size() + " duplicate ids in the data items list log file:\n" +
				Arrays.toString(DUP_ITEMS.toArray()),
			DUP_ITEMS.size() == 0
		);
	}

	@Test
	public void checkNoDuplicateTracesLogged()
	throws Exception {
		Assert.assertTrue(
			DUP_TRACES.size() + " duplicate ids in the perf trace log file:\n" +
				Arrays.toString(DUP_TRACES.toArray()),
			DUP_TRACES.size() == 0
		);
	}

	@Test
	public void checkNoMissingItemsLogged() {
		Assert.assertTrue(
			MISSING_ITEMS.size() + " missing ids in the list log file:\n" +
				Arrays.toString(MISSING_ITEMS.toArray()),
			MISSING_ITEMS.size() == 0
		);
	}
}
