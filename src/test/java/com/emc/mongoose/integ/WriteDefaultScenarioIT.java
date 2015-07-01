package com.emc.mongoose.integ;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.run.cli.ModeDispatcher;
import com.emc.mongoose.storage.mock.impl.Cinderella;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by olga on 30.06.15.
 */
public class WriteDefaultScenarioIT {

	private static Thread wsMockThread;
	private static RunTimeConfig runTimeConfig;

	@BeforeClass
	public static void before() throws Exception{
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
		//
	}

	@AfterClass
	public static void after() throws Exception{
		if (!wsMockThread.isInterrupted()) {
			wsMockThread.interrupt();
		}
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
			scenarioEndReport = "Scenario end",
			levelOfLogger = "I";

		//Get test's run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read message file and search "Scenario End"
		final File messageFile = new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "messages.log").toString());
		final BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile));
		String line;
		do {
			line = bufferedReader.readLine();
		} while ((!line.contains(scenarioEndReport)) && line != null);

		//Check the message file contain report about scenario end
		Assert.assertTrue(line.contains(scenarioEndReport));
		//Check information about "scenario end" is reported from console
		Assert.assertTrue(line.contains(levelOfLogger));
	}

	@Test
	public void shouldReportInformationAboutWritingObjectsFromConsole() throws Exception{
		final String
			levelOfLogger = "I";

		//Get test's run ID
		final String runID = RunTimeConfig.getContext().getRunId();

		//Read perf.summary file and search check log's level of summary message
		final Path perfSumFile = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, "perf.sum.csv");

		Assert.assertTrue(Files.exists(perfSumFile));
	}
}
