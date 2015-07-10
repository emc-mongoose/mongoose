package com.emc.mongoose.integ;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.integ.integTestTools.IntegConstants;
import com.emc.mongoose.integ.integTestTools.SavedOutputStream;
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by olga on 08.07.15.
 * Covers TC #5(name: "Infinite single write load job.", steps: all)
 * in Mongoose Core Functional Testing ???
 */
public class InfiniteSingleWriteScenarioIntegTest {
	//
	private static SavedOutputStream savedOutputStream;
	//
	private static String createRunId = IntegConstants.LOAD_CREATE;
	private static final String DATA_SIZE = "1B";
	//
	private static Thread writeScenarioMongoose;

	@BeforeClass
	public static void before()
		throws Exception {
		// Set new saved console output stream
		savedOutputStream = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(savedOutputStream));
		//Create run ID
		createRunId += ":infinite:" + IntegConstants.FMT_DT.format(
			Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime()
		);
		System.setProperty(RunTimeConfig.KEY_RUN_ID, createRunId);
		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(IntegConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, IntegConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(IntegConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = LogManager.getRootLogger();
		//Reload default properties
		RunTimeConfig runTimeConfig = new  RunTimeConfig();
		RunTimeConfig.setContext(runTimeConfig);
		//run mongoose default scenario in standalone mode
		writeScenarioMongoose = new Thread(new Runnable() {
			@Override
			public void run() {
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_ID, createRunId);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE);
				RunTimeConfig.getContext().set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE);
				rootLogger.info(Markers.MSG, RunTimeConfig.getContext().toString());
				new ScriptRunner().run();
			}
		}, "writeScenarioMongoose");
		writeScenarioMongoose.start();
		writeScenarioMongoose.join(15000);
		writeScenarioMongoose.interrupt();

	}

	@AfterClass
	public static void after()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SCENARIO_END_INDICATOR));
		System.setOut(savedOutputStream.getPrintStream());
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(savedOutputStream.toString().contains(IntegConstants.SUMMARY_INDICATOR));
	}

	@Test
	public void shouldStopAfterInterrupt()
	throws Exception {

	}
}
