package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.integ.integTestTools.ContentGetter;
import com.emc.mongoose.integ.integTestTools.IntegConstants;
import com.emc.mongoose.integ.integTestTools.IntegLogManager;
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;

/**
 * Created by olga on 10.07.15.
 * Covers TC #23 (name: "Write some data items.", steps: all) in Mongoose Core Functional Testing
 * HLUS: 1.5.1.3(1), 1.5.4.1, 1.5.6.1
 */
public class WriteScenarioWithNewPropertiesIntegTest {
	//
	private static SavedOutputStream savedOutputStream;
	//
	private static final int LIMIT_COUNT = 10000, LOAD_THREADS = 10;
	private static String createRunId = IntegConstants.LOAD_CREATE;
	private static final String DATA_SIZE = "0B", LIMIT_TIME = "100.minutes";

	@BeforeClass
	public static void before()
	throws Exception {
		// Set new saved console output stream
		savedOutputStream = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		//Create run ID
		createRunId += ":" + DATA_SIZE + ":" + IntegConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, createRunId);
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(IntegConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, IntegConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(IntegConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = org.apache.logging.log4j.LogManager.getRootLogger();
		//Reload default properties
		final RunTimeConfig runTimeConfig = new  RunTimeConfig();
		RunTimeConfig.setContext(runTimeConfig);
		//run mongoose default scenario in standalone mode
		final Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, createRunId);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_THREADS, LOAD_THREADS);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		writeScenarioMongoose.start();
		writeScenarioMongoose.join();
		IntegLogManager.waitLogger();
		writeScenarioMongoose.interrupt();
		System.setOut(savedOutputStream.getPrintStream());
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = IntegLogManager.getMessageFile(createRunId).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getPerfAvgFile(createRunId).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getPerfSumFile(createRunId).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getPerfTraceFile(createRunId).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getDataItemsFile(createRunId).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getErrorsFile(createRunId).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
	}

	@Test
	public void shouldCustomValuesDisplayedCorrectlyInConfigurationTable()
	throws Exception {
		final String[] runtimeConfCustomParam = RunTimeConfig.getContext().toString().split("\n");
		for (final String confParam : runtimeConfCustomParam) {
			if (confParam.contains(RunTimeConfig.KEY_API_NAME)) {
				Assert.assertTrue(confParam.contains("s3"));
			}
			if (confParam.contains(RunTimeConfig.KEY_DATA_RING_SEED)) {
				Assert.assertTrue(confParam.contains("7a42d9c483244167"));
			}
			if (confParam.contains(RunTimeConfig.KEY_DATA_RING_SIZE)) {
				Assert.assertTrue(confParam.contains(DATA_SIZE));
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_COUNT)) {
				Assert.assertTrue(confParam.contains(String.valueOf(LIMIT_COUNT)));
			}
			if (confParam.contains(RunTimeConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(confParam.contains("127.0.0.1"));
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(confParam.contains(Constants.RUN_MODE_STANDALONE));
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_ID)) {
				Assert.assertTrue(confParam.contains(createRunId));
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(confParam.contains(LIMIT_TIME));
			}
			if (confParam.contains(RunTimeConfig.KEY_SCENARIO_NAME)) {
				Assert.assertTrue(confParam.contains(IntegConstants.SCENARIO_SINGLE));
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_THREADS)) {
				Assert.assertTrue(confParam.contains(String.valueOf(LOAD_THREADS)));
			}
		}
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = IntegLogManager.getMessageFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile));
		// Search line in file which contains "Scenario end" string.
		// Get out from the loop when line with "Scenario end" if found else returned line = null
		String line;
		do {
			line = bufferedReader.readLine();
		} while ((!line.contains(IntegConstants.SCENARIO_END_INDICATOR)) && line != null);

		//Check the message file contain report about scenario end. If not line = null.
		Assert.assertTrue(line.contains(IntegConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		//Read perf.summary file of create scenario run
		final File perfSumFile = IntegLogManager.getPerfSumFile(createRunId);

		//Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();

		// Get value of "CountSucc" column
		final int actualCountSucc = Integer.valueOf(
			bufferedReader.readLine().split(",")[IntegConstants.COUNT_SUCC_COLUMN_INDEX]
		);
		Assert.assertEquals(LIMIT_COUNT, actualCountSucc);
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = IntegLogManager.getDataItemsFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		int dataSize, countDataItems = 0;
		String line = bufferedReader.readLine();

		while (line != null){
			// Get dataSize from each line
			dataSize = Integer.valueOf(line.split(",")[IntegConstants.DATA_SIZE_COLUMN_INDEX]);
			Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), dataSize);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that there are 10 lines in data.items.csv file
		Assert.assertEquals(LIMIT_COUNT, countDataItems);
	}

	@Test
	public void shouldGetDifferentObjectsFromServer()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = IntegLogManager.getDataItemsFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		String line = bufferedReader.readLine(), dataID;
		final Set setOfChecksum = new HashSet();

		while (line != null){
			dataID = line.split(",")[IntegConstants.DATA_ID_COLUMN_INDEX];
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
		final File dataItemsFile = IntegLogManager.getDataItemsFile(createRunId);
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
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		// Get data.items.csv file of write scenario run
		final File writeDataItemFile = IntegLogManager.getDataItemsFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(writeDataItemFile));
		//
		String line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(IntegLogManager.matchWithDataItemsFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file of write scenario run
		final File writePerfSumFile = IntegLogManager.getPerfSumFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(writePerfSumFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(IntegLogManager.HEADER_PERF_SUM_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(IntegLogManager.matchWithPerfSumFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFile()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File writePerfAvgFile = IntegLogManager.getPerfAvgFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(writePerfAvgFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(IntegLogManager.HEADER_PERF_AVG_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(IntegLogManager.matchWithPerfAvgFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
	throws Exception {
		// Get perf.trace.csv file of write scenario run
		final File writePerfTraceFile = IntegLogManager.getPerfTraceFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(writePerfTraceFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(IntegLogManager.HEADER_PERF_TRACE_FILE, line);
		line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(IntegLogManager.matchWithPerfTraceFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldGeneralStatusOfTheRunIsRegularlyReports()
	throws Exception {
		// Get perf.avg.csv file
		final File perfAvgFile = IntegLogManager.getPerfAvgFile(createRunId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));

		final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		Matcher matcher;
		//
		bufferedReader.readLine();
		String line = bufferedReader.readLine();
		final List<Date> listTimeOfReports = new ArrayList<>();
		while (line != null) {
			matcher = IntegConstants.TIME_PATTERN.matcher(line);
			if (matcher.find()) {
				listTimeOfReports.add(format.parse(matcher.group()));
			}
			line = bufferedReader.readLine();
		}
		// Check period of reports is correct
		long firstTime, nextTime;
		// Period must be equal 10 sec
		final int period = RunTimeConfig.getContext().getLoadMetricsPeriodSec();
		// period must be equal 10 seconds = 10000 milliseconds
		Assert.assertEquals(10, period);
		//
		for (int i = 0; i < listTimeOfReports.size() - 1; i++) {
			firstTime = listTimeOfReports.get(i).getTime();
			nextTime = listTimeOfReports.get(i + 1).getTime();
			// period must be equal 10 seconds = 10000 milliseconds
			Assert.assertEquals(10000, (nextTime - firstTime));
		}
	}
}
