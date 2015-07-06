package com.emc.mongoose.integ;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.integ.integTestTools.IntegConstants;
import com.emc.mongoose.integ.integTestTools.LogFileManager;
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
import com.emc.mongoose.integ.integTestTools.ContentGetter;
// mongoose-cli.jar
import com.emc.mongoose.run.cli.ModeDispatcher;
//
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
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
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
//
/**
 * Created by olga on 30.06.15.
 */
public final class WriteDefaultScenarioIntegTest {
	//
	private static SavedOutputStream savedOutputStream;
	//
	private static final int
		DATA_COUNT = 10,
		DATA_SIZE = 1048576;
	private static String runId;

	@BeforeClass
	public static void before()
	throws Exception{
		// Set new saved console output stream
		savedOutputStream = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(IntegConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, IntegConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(IntegConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		//final Logger rootLogger = LogManager.getRootLogger();
		RunTimeConfig.initContext();
		//run mongoose default scenario in standalone mode
		Thread writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				//Run the write default mongoose scenario
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, DATA_COUNT);
				//rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		writeScenarioMongoose.start();
		writeScenarioMongoose.join();
		writeScenarioMongoose.interrupt();
		// Set olt System.out stream
		//System.setOut(savedOutputStream.getPrintStream());
		//Get run ID
		runId =  RunTimeConfig.getContext().getRunId();
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception{
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception{
		Path expectedFile = LogFileManager.getMessageFile(runId).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getPerfAvgFile(runId).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getPerfSumFile(runId).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getPerfTraceFile(runId).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogFileManager.getDataItemsFile(runId).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception{
		//Read message file and search "Scenario End"
		final File messageFile = LogFileManager.getMessageFile(runId);
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
	throws Exception{
		//Read perf.summary file and search check log's level of summary message
		final File perfSumFile = LogFileManager.getPerfSumFile(runId);

		//Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();

		// Get value of "CountSucc" column
		final int actualCountSucc = Integer.valueOf(
			bufferedReader.readLine().split(",")[IntegConstants.COUNT_SUCC_COLUMN_INDEX]
		);
		Assert.assertEquals(DATA_COUNT, actualCountSucc);
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception{
		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = LogFileManager.getDataItemsFile(runId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		int dataSize, countDataItems = 0;
		String line = bufferedReader.readLine();

		while (line != null){
			// Get dataSize from each line
			dataSize = Integer.valueOf(line.split(",")[IntegConstants.DATA_SIZE_COLUMN_INDEX]);
			Assert.assertEquals(DATA_SIZE, dataSize);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that there are 10 lines in data.items.csv file
		Assert.assertEquals(DATA_COUNT, countDataItems);
	}

	@Test
	public void shouldGetDifferentObjectsFromServer()
	throws Exception{
		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = LogFileManager.getDataItemsFile(runId);
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
		Assert.assertEquals(setOfChecksum.size(), DATA_COUNT);
	}

	@Test
	public void shouldGetAllObjectsFromServerAndDataSizeIsDefault()
	throws Exception{
		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = LogFileManager.getDataItemsFile(runId);
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));


		String line = bufferedReader.readLine(), dataID;
		int actualDataSize;

		while (line != null){
			dataID = line.split(",")[IntegConstants.DATA_ID_COLUMN_INDEX];
			actualDataSize = ContentGetter.getDataSize(dataID);
			Assert.assertEquals(actualDataSize, DATA_SIZE);
			line = bufferedReader.readLine();
		}
	}
}
