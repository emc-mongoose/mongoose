package com.emc.mongoose.integ;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.integ.integTestTools.MD5SumJava;
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
import com.emc.mongoose.integ.integTestTools.WgetJava;
// mongoose-cli.jar
import com.emc.mongoose.run.cli.ModeDispatcher;
// mongoose-storage-mock.jar
import com.emc.mongoose.storage.mock.impl.Cinderella;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
public class WriteDefaultScenarioIT {

	private static Thread wsMockThread;
	private static RunTimeConfig runTimeConfig;
	private static SavedOutputStream savedContent = new SavedOutputStream(System.out);

	@BeforeClass
	public static void before() throws Exception{
		// Set new saved console output stream
		System.setOut(new PrintStream(savedContent));
		// If tests run from the IDEA full logging file must be set
		if (System.getProperty("log4j.configurationFile") == null) {
			String fullLogConfFile = Paths
				.get(System.getProperty("user.dir"), Constants.DIR_CONF, "logging.json")
				.toString();
			System.setProperty("log4j.configurationFile", fullLogConfFile);
		}
		//Run the WS mock
		LogUtil.init();
		RunTimeConfig.initContext();
		try {
			wsMockThread = new Thread(
				new Cinderella<>(RunTimeConfig.getContext()), "wsMock"
			);
			wsMockThread.setDaemon(true);
			wsMockThread.start();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		//Run the write default mongoose scenario
		runTimeConfig = RunTimeConfig.getContext();
		runTimeConfig.set("load.limit.count", 10);
		RunTimeConfig.setContext(runTimeConfig);
		//run mongoose default scenario in standalone mode
		ModeDispatcher.main(new String[]{"standalone"});
		// Set olt System.out stream
		System.setOut(savedContent.getPrintStream());
	}

	@AfterClass
	public static void after() throws Exception{
		if (!wsMockThread.isInterrupted()) {
			wsMockThread.interrupt();
		}
	}

	@Test
	public void shouldOutputInformationAboutSummaryFromConsole() throws Exception{
		final String
			summaryIndicator = "summary:",
			scenarioEndIndicator = "Scenario end";

		Assert.assertTrue(savedContent.toString().contains(summaryIndicator));
		Assert.assertTrue(savedContent.toString().contains(scenarioEndIndicator));
	}

	@Test
	public void shouldCreateAllFilesWithLogs() throws Exception{
		//Get test's run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		Path expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "messages.log");
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "perf.avg.csv");
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "perf.sum.csv");
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "perf.trace.csv");
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "data.items.csv");
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));
	}

	@Test
	public void shouldReportScenarioEndFromConsole() throws Exception{
		final String
			scenarioEndIndicator = "Scenario end";

		//Get test's run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read message file and search "Scenario End"
		final File messageFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "messages.log").toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile));
		// Search line in file which contains "Scenario end" string.
		// Get out from the loop when line with "Scenario end" if found else returned line = null
		String line;
		do {
			line = bufferedReader.readLine();
		} while ((!line.contains(scenarioEndIndicator)) && line != null);

		//Check the message file contain report about scenario end. If not line = null.
		Assert.assertTrue(line.contains(scenarioEndIndicator));
	}

	@Test
	public void shouldReportInformationAboutAllWritingObjectsFromSummaryFile() throws Exception{
		final int
			indexCountSucc = 8,
			expectedCountSucc = 10;

		//Get test's run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read perf.summary file and search check log's level of summary message
		final File perfSumFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "perf.sum.csv").toString());

		//Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		final BufferedReader bufferedReader = new BufferedReader(new FileReader(perfSumFile));
		bufferedReader.readLine();

		// Get value of "CountSucc" column
		final int actualCountSucc = Integer.valueOf(bufferedReader.readLine().split(",")[indexCountSucc]);

		Assert.assertEquals(actualCountSucc, expectedCountSucc);


	}

	@Test
	public void shouldCreateDataItemsFileWithInformatiomAboutAllObjects() throws Exception{
		final int indexOfDataSize = 2;
		//Get test's run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "data.items.csv").toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		int dataSize, countDataItems = 0;
		String line = bufferedReader.readLine();

		while (line != null){
			// Get dataSize from each line
			dataSize = Integer.valueOf(line.split(",")[indexOfDataSize]);
			Assert.assertEquals(dataSize, 1048576);
			countDataItems++;
			line = bufferedReader.readLine();
		}
		//Check that there are 10 lines in data.items.csv file
		Assert.assertEquals(countDataItems, 10);
	}

	@Test
	public void shouldGetDifferentObjectsFromServer() throws Exception{
		final int
			indexOfDataID = 0,
			dataCount = 10;
		//Get test's run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "data.items.csv").toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));

		String line = bufferedReader.readLine(), dataID;
		final Set setOfChecksum = new HashSet();

		while (line != null){
			dataID = line.split(",")[indexOfDataID];
			// Add each data checksum from set
			setOfChecksum.add(MD5SumJava.getMD5Checksum(dataID));
			line = bufferedReader.readLine();
		}
		// If size of set with checksums is less then dataCount it's mean that some checksums are equals
		Assert.assertEquals(setOfChecksum.size(), dataCount);
	}

	@Test
	public void shouldGetAllObjectsFromServerAndDataSizeIsDefault() throws Exception{
		final int
			indexOfDataID = 0,
			expectedDataSize = 1048576;
		//Get test's run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read data.items.csv file and search check log's level of summary message
		final File dataItemsFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "data.items.csv").toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(dataItemsFile));


		String line = bufferedReader.readLine(), dataID;
		int actualDataSize;

		while (line != null){
			dataID = line.split(",")[indexOfDataID];
			actualDataSize = WgetJava.getDataSize(dataID);
			Assert.assertEquals(actualDataSize, expectedDataSize);
			line = bufferedReader.readLine();
		}
	}
}
