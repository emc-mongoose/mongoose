package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.integTestTools.IntegConstants;
import com.emc.mongoose.integ.integTestTools.IntegLogManager;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;

/**
 * Created by olga on 10.07.15.
 * TC #12 (name: "CRUD - sequential chain scenario.", steps: all) in Mongoose Core Functional Testing
 * HLUC: 1.4.2.2, 1.5.4.5
 */
public class CRUDSequentialScenarioIntegTest {
	//
	private static SavedOutputStream savedOutputStream;
	//
	private static String chainRunId;
	private static final String
		DATA_SIZE = "10MB",
		LIMIT_TIME = "1.minutes",
		SCENARIO_NAME = "chain",
		CHAIN_LOADS =
			IntegConstants.LOAD_CREATE.toLowerCase() + "," +
			IntegConstants.LOAD_READ.toLowerCase() + "," +
			IntegConstants.LOAD_UPDATE.toLowerCase() + "," +
			IntegConstants.LOAD_DELETE.toLowerCase();
	private static final int LOAD_THREADS = 10;
	private static final int LOADS_COUNT = 4;
	private static final boolean
		CHAIN_CONCURRENT = false,
		ITEMS_BUFFER = true;

	@BeforeClass
	public static void before()
	throws Exception {
		// Set new saved console output stream
		savedOutputStream = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		//Create run ID
		chainRunId = SCENARIO_NAME + ":" + IntegConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, chainRunId);
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(IntegConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, IntegConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(IntegConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = LogManager.getRootLogger();
		//Reload default properties
		final RunTimeConfig runTimeConfig = new  RunTimeConfig();
		RunTimeConfig.setContext(runTimeConfig);
		//run mongoose default scenario in standalone mode
		final Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, chainRunId);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_NAME, SCENARIO_NAME);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD, CHAIN_LOADS);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_THREADS, LOAD_THREADS);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT, CHAIN_CONCURRENT);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_SCENARIO_CHAIN_ITEMSBUFFER, ITEMS_BUFFER);
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
		System.setOut(savedOutputStream.getPrintStream());
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = IntegLogManager.getMessageFile(chainRunId);
		Assert.assertTrue(messageFile.exists());
		//
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
	public void shouldCustomValuesDisplayedCorrectlyInConfigurationTable()
	throws Exception {
		final String[] runtimeConfCustomParam = RunTimeConfig.getContext().toString().split("\n");
		for (final String confParam : runtimeConfCustomParam) {
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_COUNT)) {
				Assert.assertTrue(confParam.contains("0"));
			}
			if (confParam.contains(RunTimeConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(confParam.contains("127.0.0.1"));
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(confParam.contains(Constants.RUN_MODE_STANDALONE));
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_ID)) {
				Assert.assertTrue(confParam.contains(chainRunId));
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(confParam.contains(LIMIT_TIME));
			}
			if (confParam.contains(RunTimeConfig.KEY_SCENARIO_NAME)) {
				Assert.assertTrue(confParam.contains(SCENARIO_NAME));
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_THREADS)) {
				Assert.assertTrue(confParam.contains(String.valueOf(LOAD_THREADS)));
			}
		}
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = IntegLogManager.getMessageFile(chainRunId).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getPerfAvgFile(chainRunId).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getPerfSumFile(chainRunId).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getPerfTraceFile(chainRunId).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = IntegLogManager.getDataItemsFile(chainRunId).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));
		/*
		expectedFile = IntegLogManager.getErrorsFile(chainRunId).toPath();
		//Check that errors.lod file isn't contained
		Assert.assertFalse(Files.exists(expectedFile));
		 */
	}

	@Test
	public void shouldCreateCorrectPerfAvgFile()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File perfAvgFile = IntegLogManager.getPerfAvgFile(chainRunId);
		Assert.assertTrue(perfAvgFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));
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
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file of write scenario run
		final File perfSumFile = IntegLogManager.getPerfSumFile(chainRunId);
		Assert.assertTrue(perfSumFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
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
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		// Get data.items.csv file of write scenario run
		final File dataItemFile = IntegLogManager.getDataItemsFile(chainRunId);
		Assert.assertTrue(dataItemFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemFile));
		//
		String line = bufferedReader.readLine();
		while (line != null) {
			Assert.assertTrue(IntegLogManager.matchWithDataItemsFilePattern(line));
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
	throws Exception {
		// Get perf.trace.csv file of write scenario run
		final File perfTraceFile = IntegLogManager.getPerfTraceFile(chainRunId);
		Assert.assertTrue(perfTraceFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfTraceFile));
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
	public void shouldContainedInformationAboutAllLoads()
		throws Exception {
		final File perfSumFile = IntegLogManager.getPerfSumFile(chainRunId);
		Assert.assertTrue(perfSumFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();
		//
		int countLinesOfSumInfo = 0;
		final Set<String> loadsSet = new HashSet<>();
		loadsSet.add(IntegConstants.LOAD_CREATE);
		loadsSet.add(IntegConstants.LOAD_READ);
		loadsSet.add(IntegConstants.LOAD_UPDATE);
		loadsSet.add(IntegConstants.LOAD_DELETE);

		String line = bufferedReader.readLine();
		while (line != null) {
			if (line.contains(IntegConstants.LOAD_CREATE) && loadsSet.contains(IntegConstants.LOAD_CREATE)) {
				loadsSet.remove(IntegConstants.LOAD_CREATE);
			}
			if (line.contains(IntegConstants.LOAD_READ) && loadsSet.contains(IntegConstants.LOAD_READ)) {
				loadsSet.remove(IntegConstants.LOAD_READ);
			}
			if (line.contains(IntegConstants.LOAD_UPDATE) && loadsSet.contains(IntegConstants.LOAD_UPDATE)) {
				loadsSet.remove(IntegConstants.LOAD_UPDATE);
			}
			if (line.contains(IntegConstants.LOAD_DELETE) && loadsSet.contains(IntegConstants.LOAD_DELETE)) {
				loadsSet.remove(IntegConstants.LOAD_DELETE);
			}
			countLinesOfSumInfo ++;
			line = bufferedReader.readLine();
		}
		Assert.assertTrue(loadsSet.isEmpty());
		Assert.assertEquals(LOADS_COUNT, countLinesOfSumInfo);
	}

	@Test
	public void shouldCreateCorrectInformationAboutLoad()
	throws Exception {
		// Get perf.avg.csv file of write scenario run
		final File writePerfAvgFile = IntegLogManager.getPerfAvgFile(chainRunId);
		Assert.assertTrue(writePerfAvgFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(writePerfAvgFile));
		//
		String line = bufferedReader.readLine();
		//Check that header of file is correct
		Assert.assertEquals(IntegLogManager.HEADER_PERF_AVG_FILE, line);
		//
		Matcher matcher, loadTypeMatcher;
		String actualLoadType, apiName;
		String[] loadInfo;
		int threadsPerNode, countNode;
		//
		line = bufferedReader.readLine();
		while (line != null) {
			//
			matcher = IntegConstants.LOAD_PATTERN.matcher(line);
			if (matcher.find()) {
				loadInfo = matcher.group().split("(-|x)");
				//Check api name is correct
				apiName = loadInfo[1].toLowerCase();
				Assert.assertEquals(IntegConstants.API_S3, apiName);
				// Check load type and load limit count values are correct
				actualLoadType = loadInfo[2];
				loadTypeMatcher = IntegConstants.LOAD_NAME_PATTERN.matcher(actualLoadType);
				Assert.assertTrue(loadTypeMatcher.find());
				// Check "threads per node" value is correct
				threadsPerNode = Integer.valueOf(loadInfo[3]);
				Assert.assertEquals(LOAD_THREADS , threadsPerNode);
				//Check node count is correct
				countNode = Integer.valueOf(loadInfo[4]);
				Assert.assertEquals( 1 , countNode);
			}
			line = bufferedReader.readLine();
		}
	}

	@Test
	public void shouldPerfSumFileContainsSummaryInfoAboutAllLoads()
	throws Exception {
		final File perfSumFile = IntegLogManager.getPerfSumFile(chainRunId);
		Assert.assertTrue(perfSumFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		//
		bufferedReader.readLine();
		String line = bufferedReader.readLine();
		int countLinesOfSumInfo = 0;
		while (line != null) {
			line = bufferedReader.readLine();
			countLinesOfSumInfo ++;
		}
		Assert.assertEquals(LOADS_COUNT, countLinesOfSumInfo);
	}

	@Test
	public void shouldLoadsSwitchOperationsSynchronously()
	throws Exception {
		final File perfAvgFile = IntegLogManager.getPerfAvgFile(chainRunId);
		Assert.assertTrue(perfAvgFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));
		//
		Matcher matcher;
		int counterSwitch = 1;
		String
			line = bufferedReader.readLine(),
			prevLoadType = IntegConstants.LOAD_CREATE;
		while (line != null) {
			matcher = IntegConstants.LOAD_NAME_PATTERN.matcher(line);
			if (matcher.find()){
				if (!prevLoadType.equals(matcher.group())) {
					counterSwitch++;
					prevLoadType = matcher.group();
				}
			}
			line = bufferedReader.readLine();
		}
		Assert.assertEquals(LOADS_COUNT, counterSwitch);
	}

	@Test
	public void shouldGeneralStatusOfTheRunIsRegularlyReports()
	throws Exception {
		final int precisionMillis = 1000;
		// Get perf.avg.csv file
		final File perfAvgFile = IntegLogManager.getPerfAvgFile(chainRunId);
		Assert.assertTrue(perfAvgFile.exists());
		//
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));

		final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		Matcher matcherDate, matcherLoadType;
		//
		bufferedReader.readLine();
		String line = bufferedReader.readLine();
		String loadType, date;
		// map by load type
		final Map<String, ArrayList<Date>> mapReports = new HashMap<>(4);
		while (line != null) {
			matcherDate = IntegConstants.TIME_PATTERN.matcher(line);
			matcherLoadType = IntegConstants.LOAD_NAME_PATTERN.matcher(line);
			if (matcherDate.find() && matcherLoadType.find()) {
				loadType = matcherLoadType.group();
				date = matcherDate.group();
				if (mapReports.containsKey(loadType)) {
					final List<Date> listReports = mapReports.get(loadType);
					listReports.add(format.parse(date));
					mapReports.put(loadType, (ArrayList<Date>) listReports);
				} else {
					mapReports.put(loadType, new ArrayList<>(Arrays.asList(format.parse(date))));
				}
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
		for (final String mapLoadType : mapReports.keySet()) {
			final List<Date> listReports = mapReports.get(mapLoadType);
			for (int i = 0; i < listReports.size() - 1; i++) {
				firstTime = listReports.get(i).getTime();
				nextTime = listReports.get(i + 1).getTime();
				// period must be equal 10 seconds = 10000 milliseconds
				Assert.assertTrue(
					10000 - precisionMillis < (nextTime - firstTime) &&
					10000 + precisionMillis > (nextTime - firstTime)
				);
			}
		}
	}

	@Test
	public void shouldCreateLoadMustRunFor60Seconds()
	throws Exception {
		final File perfAvgFile = IntegLogManager.getPerfAvgFile(chainRunId);
		Assert.assertTrue(perfAvgFile.exists());
		//
		BufferedReader bufferedReader = new BufferedReader(new FileReader(perfAvgFile));
		//
		Date startTimeLoad = null, finishTimeLoad = null;
		final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		Matcher matcherDate, matcherLoadName;
		// Get start time of loads
		bufferedReader.readLine();
		String line = bufferedReader.readLine();
		while (line != null) {
			matcherDate = IntegConstants.TIME_PATTERN.matcher(line);
			matcherLoadName = IntegConstants.LOAD_NAME_PATTERN.matcher(line);
			if (matcherDate.find() && matcherLoadName.find()) {
				if (matcherLoadName.group().equals(IntegConstants.LOAD_CREATE)) {
					startTimeLoad =format.parse(matcherDate.group());
					break;
				}
			}
			line = bufferedReader.readLine();
		}
		// Get finish time of loads
		final File perfSumFile = IntegLogManager.getPerfSumFile(chainRunId);
		Assert.assertTrue(perfSumFile.exists());
		//
		bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();
		line = bufferedReader.readLine();
		while (line != null) {
			matcherDate = IntegConstants.TIME_PATTERN.matcher(line);
			matcherLoadName = IntegConstants.LOAD_NAME_PATTERN.matcher(line);
			if (matcherDate.find() && matcherLoadName.find()) {
				if (matcherLoadName.group().equals(IntegConstants.LOAD_CREATE)) {
					finishTimeLoad = format.parse(matcherDate.group());
					break;
				}
			}
			line = bufferedReader.readLine();
		}
		// 1.minutes = 60000.milliseconds
		final int precisionMillis = 2000, loadLimitTimeMillis = 60000;
		long differenceTime = finishTimeLoad.getTime() - startTimeLoad.getTime();
		Assert.assertTrue(
			differenceTime > loadLimitTimeMillis - precisionMillis &&
			differenceTime < loadLimitTimeMillis + precisionMillis
		);
	}

	@Test
	public void shouldDataItemsMasksAreUpdate()
	throws Exception {
		final File dataItemsFile = IntegLogManager.getDataItemsFile(chainRunId);
		//
		Assert.assertTrue(dataItemsFile.exists());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		final int firstMaskVal = 0;
		int maskVal;
		String line = bufferedReader.readLine();

		while (line != null) {
			maskVal = Integer.valueOf(line.split("(/)")[1]);
			// Check that data items masks are update and not equal 0
			Assert.assertTrue(maskVal != firstMaskVal);
			line = bufferedReader.readLine();
		}
	}
}
