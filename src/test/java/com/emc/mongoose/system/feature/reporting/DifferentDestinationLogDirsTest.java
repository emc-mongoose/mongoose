package com.emc.mongoose.system.feature.reporting;

import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
import com.emc.mongoose.system.base.HttpStorageMockTestBase;
import com.emc.mongoose.system.tools.LogValidator;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 15.06.16.
 */
public class DifferentDestinationLogDirsTest
extends HttpStorageMockTestBase {

	private final static String RUN_ID_SEQ[] = new String[] {
		"runId0", "runId1", "runId2"
	};
	private final static String SCENARIO_TEXT =
		"{\n" +
		"	\"type\" : \"parallel\",\n" +
		"	\"config\" : {\n" +
		"		\"load\" : {\n" +
		"			\"limit\" : {\n" +
		"				\"time\" : \"30s\"\n" +
		"			}\n" +
		"		}\n" +
		"	},\n" +
		"	\"jobs\" : [\n" +
		"		{\n" +
		"			\"type\" : \"load\",\n" +
		"			\"config\" : {\n" +
		"				\"run\" : { \"id\" : \"" + RUN_ID_SEQ[0] + "\" }\n" +
		"			}\n" +
		"		}, {\n" +
		"			\"type\" : \"load\",\n" +
		"			\"config\" : {\n" +
		"				\"run\" : { \"id\" : \"" + RUN_ID_SEQ[1] + "\" }\n" +
		"			}\n" +
		"		}, {\n" +
		"			\"type\" : \"load\",\n" +
		"			\"config\" : {\n" +
		"				\"run\" : { \"id\" : \"" + RUN_ID_SEQ[2] + "\" }\n" +
		"			}\n" +
		"		}\n" +
		"	]\n" +
		"}\n";

	@BeforeClass
	public static void setUpClass() {
		for(final String nextRunId : RUN_ID_SEQ) {
			try {
				LogValidator.removeLogDirectory(nextRunId);
			} catch(final Exception e) {
				e.printStackTrace(System.out);
			}
		}
		try {
			HttpStorageMockTestBase.setUpClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
		try(
			final Scenario scenario = new JsonScenario(
				BasicConfig.THREAD_CONTEXT.get(), SCENARIO_TEXT
			)
		) {
			scenario.run();
			TimeUnit.SECONDS.sleep(1);
			RunIdFileManager.flushAll();
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
	}

	@AfterClass
	public static void tearDownClass() {
		try {
			HttpStorageMockTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
	}

	@Test
	public void checkItemsCsvFiles()
	throws Exception {
		File nextItemsCsvFile;
		for(final String nextRunId : RUN_ID_SEQ) {
			nextItemsCsvFile = LogValidator.getItemsListFile(nextRunId);
			Assert.assertTrue(
				nextItemsCsvFile.toString() + " doesn't exist", nextItemsCsvFile.exists()
			);
		}
	}

	@Test
	public void checkMessagesLogFiles()
	throws Exception {
		File nextMessagesLogFile;
		for(final String nextRunId : RUN_ID_SEQ) {
			nextMessagesLogFile = LogValidator.getMessageLogFile(nextRunId);
			Assert.assertTrue(nextMessagesLogFile.exists());
		}
	}

	@Test
	public void checkPerfAvgCsvFiles()
	throws Exception {
		File nextPerfAvgCsvFile;
		for(final String nextRunId : RUN_ID_SEQ) {
			nextPerfAvgCsvFile = LogValidator.getPerfAvgFile(nextRunId);
			Assert.assertTrue(nextPerfAvgCsvFile.exists());
		}
	}

	@Test
	public void checkPerfSumCsvFiles()
	throws Exception {
		File nextPerfSumCsvFile;
		for(final String nextRunId : RUN_ID_SEQ) {
			nextPerfSumCsvFile = LogValidator.getPerfSumFile(nextRunId);
			Assert.assertTrue(nextPerfSumCsvFile.exists());
		}
	}

	@Test
	public void checkPerfTraceCsvFiles()
	throws Exception {
		File nextPerfTraceCsvFile;
		for(final String nextRunId : RUN_ID_SEQ) {
			nextPerfTraceCsvFile = LogValidator.getPerfTraceFile(nextRunId);
			Assert.assertTrue(nextPerfTraceCsvFile.exists());
		}
	}
}
