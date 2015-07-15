package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.tools.ContentGetter;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.SavedOutputStream;
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
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
 * HLUC: 1.1.3.1
 */
public class WriteRandomSizedItemsTest {
	//
	private static SavedOutputStream savedOutputStream;
	//
	private static final int LIMIT_COUNT = 10;
	private static String createRunId = TestConstants.LOAD_CREATE;
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
		createRunId += ":" + DATA_SIZE_MIN + "-" + DATA_SIZE_MAX + ":" + TestConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, createRunId);
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(TestConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, TestConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(TestConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = org.apache.logging.log4j.LogManager.getRootLogger();
		// Reload default properties
		final RunTimeConfig runTimeConfig = new  RunTimeConfig();
		RunTimeConfig.setContext(runTimeConfig);
		// Run mongoose default scenario in standalone mode
		final Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, createRunId);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE_MIN);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE_MAX);
				// For correct work of verification option
				UniformDataSource.DEFAULT = new UniformDataSource();
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		writeScenarioMongoose.start();
		writeScenarioMongoose.join();
		writeScenarioMongoose.interrupt();
		// Wait logger's output from console
		Thread.sleep(3000);
		System.setOut(savedOutputStream.getReplacedStream());
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(TestConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(savedOutputStream.toString().contains(TestConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = LogParser.getMessageFile(createRunId);
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
	public void shouldCreateAllFilesWithLogs()
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
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		// Get data.items.csv file of write scenario run
		final File dataItemFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemFile));
		//
		String line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogParser.matchWithDataItemsFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file of write scenario run
		final File perfSumFile = LogParser.getPerfSumFile(createRunId);
		Assert.assertTrue(perfSumFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(LogParser.HEADER_PERF_SUM_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogParser.matchWithPerfSumFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFile()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = LogParser.getPerfAvgFile(createRunId);
		Assert.assertTrue(perfAvgFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(LogParser.HEADER_PERF_AVG_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogParser.matchWithPerfAvgFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
	throws Exception {
		// Get perf.trace.csv file of write scenario run
		final File perfTraceFile = LogParser.getPerfTraceFile(createRunId);
		Assert.assertTrue(perfTraceFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfTraceFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(LogParser.HEADER_PERF_TRACE_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(LogParser.matchWithPerfTraceFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		// Read perf.summary file of create scenario run
		final File perfSumFile = LogParser.getPerfSumFile(createRunId);

		// Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();

		// Get value of "CountSucc" column
		final int actualCountSucc = Integer.valueOf(
			bufferedReader.readLine().split(",")[TestConstants.COUNT_SUCC_COLUMN_INDEX]
		);
		Assert.assertEquals(LIMIT_COUNT, actualCountSucc);
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		// Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemsFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		int dataSize, countDataItems = 0, actualMinSize = Integer.MAX_VALUE, actualMaxSize = 0;
		String line = bufferedReader.readLine();
		// Size of this set must be > 1. It's mean that there are different data items sizes
		final Set<Integer> dataItemSizes = new HashSet<>();

		//
		while (line != null) {
			// Get dataSize from each line
			dataSize = Integer.valueOf(line.split(",")[TestConstants.DATA_SIZE_COLUMN_INDEX]);
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
		Assert.assertEquals(LIMIT_COUNT, countDataItems);
		// Check that there are different data sizes
		Assert.assertTrue(dataItemSizes.size() > 1);
		// Check data items min size is correct
		Assert.assertTrue(SizeUtil.toSize(DATA_SIZE_MIN) <= actualMinSize);
		// Check data items max size is correct
		Assert.assertTrue(SizeUtil.toSize(DATA_SIZE_MAX) >= actualMaxSize);
	}

	@Test
	public void shouldGetDifferentObjectsFromServer()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(createRunId);
		Assert.assertTrue(dataItemsFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		String line = bufferedReader.readLine(), dataID;
		final Set setOfChecksum = new HashSet();

		while (line != null){
			dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
			// Add each data checksum from set
			try (final InputStream inputStream = ContentGetter.getStream(dataID)) {
				setOfChecksum.add(DigestUtils.md2Hex(inputStream));
			}
			line = bufferedReader.readLine();
		}
		// If size of set with checksums is less then dataCount it's mean that some checksums are equals
		Assert.assertEquals(LIMIT_COUNT, setOfChecksum.size());
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
		int actualDataSize, expectedDataSize;
		String[] dataItemInfo;

		while (line != null){
			dataItemInfo = line.split(",");
			dataID = dataItemInfo[TestConstants.DATA_ID_COLUMN_INDEX];
			expectedDataSize = Integer.valueOf(dataItemInfo[TestConstants.DATA_SIZE_COLUMN_INDEX]);
			actualDataSize = ContentGetter.getDataSize(dataID);
			Assert.assertEquals(expectedDataSize, actualDataSize);
			line = bufferedReader.readLine();
		}
	}
}
