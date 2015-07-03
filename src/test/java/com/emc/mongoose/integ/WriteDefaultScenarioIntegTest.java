package com.emc.mongoose.integ;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
import com.emc.mongoose.integ.integTestTools.ContentGetter;
// mongoose-cli.jar
import com.emc.mongoose.run.cli.ModeDispatcher;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.impl.Cinderella;
//
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
//
/**
 * Created by olga on 30.06.15.
 */
public class WriteDefaultScenarioIntegTest {

	private static Thread wsMockThread;
	private static RunTimeConfig runTimeConfig;
	private static SavedOutputStream savedContent = new SavedOutputStream(System.out);
	//
	private static final String
		LOG_CONF_PROPERTY_KEY = "log4j.configurationFile",
		LOG_FILE_NAME = "logging.json",
		USER_DIR_PROPERTY_NAME = "user.dir",
		SUMMARY_INDICATOR = "summary:",
		SCENARIO_END_INDICATOR = "Scenario end",
		//
		MESSAGE_FILE_NAME = "messages.log",
		PERF_AVG_FILE_NAME = "perf.avg.csv",
		PERF_SUM_FILE_NAME = "perf.sum.csv",
		PERF_TRACE_FILE_NAME = "perf.trace.csv",
		DATA_ITEMS_FILE_NAME = "data.items.csv";
	//
	private static int
		DATA_COUNT = 10,
		COUNT_SUCC_COLUMN_INDEX = 8,
		DATA_SIZE_COLUMN_INDEX = 2,
		DATA_ID_COLUMN_INDEX = 0,
		DATA_SIZE = 1048576;

	@BeforeClass
	public static void before()
	throws Exception{
		// Set new saved console output stream
		System.setOut(new PrintStream(savedContent));
		// If tests run from the IDEA full logging file must be set
		if (System.getProperty(LOG_CONF_PROPERTY_KEY) == null) {
			String fullLogConfFile = Paths
				.get(System.getProperty(USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, LOG_FILE_NAME)
				.toString();
			System.setProperty(LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		}
		//Run the WS mock
		LogUtil.init();
		RunTimeConfig.initContext();
		wsMockThread = new Thread(
			new Cinderella<>(RunTimeConfig.getContext()), "wsMock"
		);
		wsMockThread.setDaemon(true);
		wsMockThread.start();
		//Run the write default mongoose scenario
		runTimeConfig = RunTimeConfig.getContext();
		runTimeConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, DATA_COUNT);
		RunTimeConfig.setContext(runTimeConfig);
		//run mongoose default scenario in standalone mode
		ModeDispatcher.main(new String[]{"standalone"});
		// Set olt System.out stream
		System.setOut(savedContent.getPrintStream());
	}

	@AfterClass
	public static void after()
	throws Exception{
		if (!wsMockThread.isInterrupted()) {
			wsMockThread.interrupt();
		}
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception{
		Assert.assertTrue(savedContent.toString().contains(SUMMARY_INDICATOR));
		Assert.assertTrue(savedContent.toString().contains(SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception{
		//Get run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		Path expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, MESSAGE_FILE_NAME);
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, PERF_AVG_FILE_NAME);
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, PERF_SUM_FILE_NAME);
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, PERF_TRACE_FILE_NAME);
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, DATA_ITEMS_FILE_NAME);
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));
	}

	@Test
	public void shouldReportScenarioEndFromConsole()
	throws Exception{
		//Get run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read message file and search "Scenario End"
		final File messageFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, MESSAGE_FILE_NAME).toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile));
		// Search line in file which contains "Scenario end" string.
		// Get out from the loop when line with "Scenario end" if found else returned line = null
		String line;
		do {
			line = bufferedReader.readLine();
		} while ((!line.contains(SCENARIO_END_INDICATOR)) && line != null);

		//Check the message file contain report about scenario end. If not line = null.
		Assert.assertTrue(line.contains(SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception{
		//Get run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read perf.summary file and search check log's level of summary message
		final File perfSumFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, PERF_SUM_FILE_NAME).toString());

		//Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();

		// Get value of "CountSucc" column
		final int actualCountSucc = Integer.valueOf(bufferedReader.readLine().split(",")[COUNT_SUCC_COLUMN_INDEX]);

		Assert.assertEquals(actualCountSucc, DATA_COUNT);
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception{
		//Get run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, DATA_ITEMS_FILE_NAME).toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		int dataSize, countDataItems = 0;
		String line = bufferedReader.readLine();

		while (line != null){
			// Get dataSize from each line
			dataSize = Integer.valueOf(line.split(",")[DATA_SIZE_COLUMN_INDEX]);
			Assert.assertEquals(dataSize, DATA_SIZE);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that there are 10 lines in data.items.csv file
		Assert.assertEquals(countDataItems, DATA_COUNT);
	}

	@Test
	public void shouldGetDifferentObjectsFromServer()
	throws Exception{
		//Get run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, DATA_ITEMS_FILE_NAME).toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		String line = bufferedReader.readLine(), dataID;
		final Set setOfChecksum = new HashSet();

		while (line != null){
			dataID = line.split(",")[DATA_ID_COLUMN_INDEX];
			// Add each data checksum from set
			try (final InputStream inputStream = ContentGetter.getStream(dataID)) {
				setOfChecksum.add(DigestUtils.md2Hex(inputStream));
			}
			line = bufferedReader.readLine();
		}
		// If size of set with checksums is less then dataCount it's mean that some checksums are equals
		Assert.assertEquals(setOfChecksum.size(), DATA_COUNT);
	}

	@Test
	public void shouldGetAllObjectsFromServerAndDataSizeIsDefault()
	throws Exception{
		//Get run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, DATA_ITEMS_FILE_NAME).toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));


		String line = bufferedReader.readLine(), dataID;
		int actualDataSize;

		while (line != null){
			dataID = line.split(",")[DATA_ID_COLUMN_INDEX];
			actualDataSize = ContentGetter.getDataSize(dataID);
			Assert.assertEquals(actualDataSize, DATA_SIZE);
			line = bufferedReader.readLine();
		}
	}
}
