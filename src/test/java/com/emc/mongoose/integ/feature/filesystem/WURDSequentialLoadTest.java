package com.emc.mongoose.integ.feature.filesystem;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.integ.base.LoggingTestBase;
import com.emc.mongoose.integ.tools.StdOutUtil;
import com.emc.mongoose.integ.tools.LogPatterns;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.run.scenario.runner.ScenarioRunner;
import org.apache.commons.codec.binary.Hex;
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
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * Created by olga on 10.07.15.
 * TC #12 (name: "CRUD - sequential chain scenario.", steps: all) in Mongoose Core Functional Testing
 * HLUC: 1.4.2.2, 1.5.4.5
 */
public class WURDSequentialLoadTest
extends LoggingTestBase {

	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static String RUN_ID = WURDSequentialLoadTest.class.getCanonicalName();
	private static final String
		DATA_SIZE = "64KB",
		SCENARIO_NAME = "chain",
		CHAIN_LOADS =
			TestConstants.LOAD_CREATE.toLowerCase() + "," +
			TestConstants.LOAD_UPDATE.toLowerCase() + "," +
			TestConstants.LOAD_READ.toLowerCase() + "," +
			TestConstants.LOAD_DELETE.toLowerCase();
	private static final long LOAD_CONNS = 10;
	private static final long LOADS_COUNT = 4;
	private static final boolean CHAIN_CONCURRENT = false;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		LoggingTestBase.setUpClass();
		//
		final AppConfig appConfig = BasicConfig.CONTEXT_CONFIG.get();
		appConfig.set(RunTimeConfig.KEY_ITEM_CLASS, "file");
		appConfig.set(RunTimeConfig.KEY_ITEM_PREFIX, "/tmp/" + RUN_ID);
		appConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
		appConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
		appConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, 10000);
		appConfig.set(RunTimeConfig.KEY_SCENARIO_NAME, SCENARIO_NAME);
		appConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD, CHAIN_LOADS);
		appConfig.set(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT, String.valueOf(CHAIN_CONCURRENT));
		appConfig.set(RunTimeConfig.KEY_CREATE_CONNS, String.valueOf(LOAD_CONNS));
		appConfig.set(RunTimeConfig.KEY_READ_CONNS, String.valueOf(LOAD_CONNS));
		appConfig.set(RunTimeConfig.KEY_UPDATE_CONNS, String.valueOf(LOAD_CONNS));
		appConfig.set(RunTimeConfig.KEY_DELETE_CONNS, String.valueOf(LOAD_CONNS));
		appConfig.set(RunTimeConfig.KEY_APPEND_CONNS, String.valueOf(LOAD_CONNS));
		appConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		RunTimeConfig.setContext(appConfig);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, BasicConfig.CONTEXT_CONFIG.get().toString());
		//
		try(
			final BufferingOutputStream
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
	}

	@AfterClass
	public  static void tearDownClass()
	throws Exception {
		LoggingTestBase.tearDownClass();
		System.setProperty(RunTimeConfig.KEY_ITEM_CLASS, "data");
		System.setProperty(RunTimeConfig.KEY_ITEM_PREFIX, "");
		LoggingTestBase.tearDownClass();
		final File tgtDir = Paths.get("/tmp/" + RUN_ID).toFile();
		for(final File f : tgtDir.listFiles()) {
			f.delete();
		}
		tgtDir.delete();
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
			     new BufferedReader(new FileReader(messageFile))
		) {
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
	public void shouldCustomValuesDisplayedCorrectlyInConfigurationTable()
	throws Exception {
		final String configTable = BasicConfig.CONTEXT_CONFIG.get().toString();
		final Set<String> params = new HashSet<>();
		//  skip table header
		int start = 126;
		int lineOffset = 100;
		while (start + lineOffset < configTable.length()) {
			params.add(configTable.substring(start, start + lineOffset));
			start += lineOffset;
		}
		for (final String confParam : params) {
			if (confParam.contains(RunTimeConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(
					"Information about storage address in configuration table is wrong",
					confParam.contains("127.0.0.1")
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(
					"Information about run mode in configuration table is wrong",
					confParam.contains(Constants.RUN_MODE_STANDALONE)
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_ID)) {
				if (RUN_ID.length() >= 64) {
					Assert.assertTrue(
						"Information about run id in configuration table is wrong",
						confParam.contains(RUN_ID.substring(0, 63).trim())
					);
				} else {
					Assert.assertTrue(
						"Information about run id in configuration table is wrong",
						confParam.contains(RUN_ID)
					);
				}
			}
			if (confParam.contains(RunTimeConfig.KEY_SCENARIO_NAME)) {
				Assert.assertTrue(
					"Information about scenario name in configuration table is wrong",
					confParam.contains(TestConstants.SCENARIO_CHAIN)
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_WORKERS)) {
				Assert.assertTrue(
					"Information about load threads in configuration table is wrong",
					confParam.contains(String.valueOf(LOAD_CONNS))
				);
			}
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
	public void shouldCreateCorrectPerfAvgFile()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = LogValidator.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("perfAvg.csv file doesn't exist", perfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfAvgCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file
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
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		// Get data.items.csv file
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
	public void shouldCreateCorrectPerfTraceFile()
	throws Exception {
		// Get perf.trace.csv file
		final File perfTraceFile = LogValidator.getPerfTraceFile(RUN_ID);
		Assert.assertTrue("perf.trace.csv file doesn't exist", perfTraceFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfTraceFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogValidator.assertCorrectPerfTraceCSV(in);
		}
	}

	@Test
	public void shouldContainedInformationAboutAllLoads()
	throws Exception {
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file doesn't exist", perfSumFile.exists());
		//
		try (final BufferedReader
			     bufferedReader = new BufferedReader(new FileReader(perfSumFile))
		) {
			//  read header of csv file
			bufferedReader.readLine();

			int countLinesOfSumInfo = 0;
			final Set<String> loadsSet = new HashSet<>();
			loadsSet.add(TestConstants.LOAD_CREATE);
			loadsSet.add(TestConstants.LOAD_READ);
			loadsSet.add(TestConstants.LOAD_UPDATE);
			loadsSet.add(TestConstants.LOAD_DELETE);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TestConstants.LOAD_CREATE)
					&& loadsSet.contains(TestConstants.LOAD_CREATE)) {
					loadsSet.remove(TestConstants.LOAD_CREATE);
				}
				if (line.contains(TestConstants.LOAD_READ)
					&& loadsSet.contains(TestConstants.LOAD_READ)) {
					loadsSet.remove(TestConstants.LOAD_READ);
				}
				if (line.contains(TestConstants.LOAD_UPDATE)
					&& loadsSet.contains(TestConstants.LOAD_UPDATE)) {
					loadsSet.remove(TestConstants.LOAD_UPDATE);
				}
				if (line.contains(TestConstants.LOAD_DELETE)
					&& loadsSet.contains(TestConstants.LOAD_DELETE)) {
					loadsSet.remove(TestConstants.LOAD_DELETE);
				}
				countLinesOfSumInfo++;
			}
			Assert.assertTrue(
				"There aren't all summary statmens in perf.sum.csv file",
				loadsSet.isEmpty()
			);
			Assert.assertEquals(
				"Count of loads is wrong", LOADS_COUNT, countLinesOfSumInfo
			);
		}
	}

	@Test
	public void shouldCreateCorrectInformationAboutLoad()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = LogValidator.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("perfAvg.csv file doesn't exist", perfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			Matcher matcher;
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			String actualNodesCount; int actualConnectionsCount;
			for(final CSVRecord nextRec : recIter) {
				if (firstRow) {
					firstRow = false;
				} else {
					Assert.assertEquals(
						"Storage API is wrong: " + nextRec.toString(), "fs", nextRec.get(2).toLowerCase()
					);
					matcher = LogPatterns.TYPE_LOAD.matcher(nextRec.get(3));
					Assert.assertTrue(
						"Type load is wrong: " + nextRec.toString(), matcher.find()
					);
					actualConnectionsCount = Integer.valueOf(nextRec.get(4));
					Assert.assertEquals(
						"Count of connections is wrong: " + nextRec.toString(), LOAD_CONNS, actualConnectionsCount
					);
					actualNodesCount = nextRec.get(5);
					if(actualNodesCount != null && !actualNodesCount.isEmpty()) {
						Assert.assertEquals(
							"Count of nodes is wrong: " + nextRec.toString(),
							0, Integer.valueOf(actualNodesCount).intValue()
						);
					}
				}
			}
		}
	}


	@Test
	public void shouldLoadsSwitchOperationsSynchronously()
	throws Exception {
		final File perfAvgFile = LogValidator.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("perf.avg.csv file doesn't exist", perfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			int counterSwitch = 1;
			String prevLoadType = TestConstants.LOAD_CREATE;
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			//
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
				} else {
					if(!prevLoadType.equals(nextRec.get(3))) {
						counterSwitch ++;
						prevLoadType = nextRec.get(3);
					}
				}
			}
			Assert.assertEquals("Loads don't switch synchronously", LOADS_COUNT, counterSwitch);
		}
	}

	@Test
	public void checkItemsMasksUpdated()
	throws Exception {
		final File dataItemsFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("items.csv file doesn't exist", dataItemsFile.exists());

		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemsFile.toPath(), StandardCharsets.UTF_8)
		) {
			BitSet mask;
			String rangesMask;
			char rangesMaskChars[];
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				rangesMask = nextRec.get(3).split("\\/")[1];
				if(rangesMask.length() == 0) {
					rangesMaskChars = ("00" + rangesMask).toCharArray();
				} else if(rangesMask.length() % 2 == 1) {
					rangesMaskChars = ("0" + rangesMask).toCharArray();
				} else {
					rangesMaskChars = rangesMask.toCharArray();
				}
				mask = BitSet.valueOf(Hex.decodeHex(rangesMaskChars));
				Assert.assertTrue(nextRec.toString(), mask.cardinality() > 0);
			}
		}
	}
}
