package com.emc.mongoose.system.feature.reporting;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
import com.emc.mongoose.system.base.HttpStorageMockTestBase;
import com.emc.mongoose.system.tools.LogValidator;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
/**
 Created by andrey on 28.06.16.
 */
public class JiraIssue729RegressionTest
extends HttpStorageMockTestBase {

	private final static String RUN_ID_BASE = JiraIssue729RegressionTest.class.getCanonicalName();
	private final static String STORAGE_ADDRS = "127.0.0.1:9020,127.0.0.1:9022,127.0.0.1:9023";
	private final static int CONCURRENCY_PER_NODE = 100;
	private final static int ITEM_SIZE = 10240;
	private final static int LOAD_TIME_LIMIT_SECONDS = 30;
	private final static String SCENARIO_JSON =
		"{" +
		"	\"type\" : \"sequential\",\n" +
		"	\"jobs\" : [\n" +
		"		{\n" +
		"			\"type\" : \"load\",\n" +
		"			\"config\" : {\n" +
		"				\"item\" : {\n" +
		"					\"dst\" : {\n" +
		"						\"file\" : \"" + RUN_ID_BASE  + ".csv\"\n" +
		"					}\n" +
		"				},\n" +
		"				\"load\" : {\n" +
		"					\"limit\" : {\n" +
		"						\"count\": 100\n" +
		"					},\n" +
		"					\"type\" : \"create\"\n" +
		"				}\n" +
		"			}\n" +
		"		}, {\n" +
		"			\"type\" : \"command\",\n" +
		"			\"value\" : \"sleep 10\"\n" +
		"		}, {\n" +
		"			\"type\" : \"for\",\n" +
		"			\"value\" : \"i\",\n" +
		"			\"in\" : [ 0, 1, 2, 3 ],\n" +
		"			\"config\" : {\n" +
		"				\"item\" : {\n" +
		"					\"src\" : {\n" +
		"						\"file\" : \"" + RUN_ID_BASE  + ".csv\"\n" +
		"					}\n" +
		"				},\n" +
		"				\"load\" : {\n" +
		"					\"circular\" : true,\n" +
		"					\"limit\" : {\n" +
		"						\"time\": \"" + LOAD_TIME_LIMIT_SECONDS + "\"\n" +
		"					},\n" +
		"					\"type\" : \"read\"\n" +
		"				},\n" +
		"				\"run\" : {\n" +
		"					\"id\" : \"" + RUN_ID_BASE + "_${i}\"\n" +
		"				}\n" +
		"			},\n" +
		"			\"jobs\" : [\n" +
		"				{\n" +
		"					\"type\" : \"load\"\n" +
		"				}\n" +
		"			]\n" +
		"		}\n" +
		"	]\n" +
		"}\n";

	@BeforeClass
	public static void setUpClass() {

		try {
			LogValidator.removeLogDirectory(RUN_ID_BASE + "Create");
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
		for(int i = 0; i < 10; i ++) {
			try {
				LogValidator.removeLogDirectory(RUN_ID_BASE + "_" + i);
			} catch(final Exception e) {
				e.printStackTrace(System.out);
			}
		}
		try {
			Files.delete(Paths.get(RUN_ID_BASE  + ".csv"));
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}

		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID_BASE + "Create");
		System.setProperty(AppConfig.KEY_STORAGE_ADDRS, STORAGE_ADDRS);
		System.setProperty(AppConfig.KEY_LOAD_THREADS, Integer.toString(CONCURRENCY_PER_NODE));
		System.setProperty(AppConfig.KEY_ITEM_DATA_SIZE, Integer.toString(ITEM_SIZE));
		System.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, RUN_ID_BASE);

		try {
			HttpStorageMockTestBase.setUpClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to init the HTTP storage mock");
			e.printStackTrace(System.out);
		}

		try(
			final Scenario scenario = new JsonScenario(
				BasicConfig.THREAD_CONTEXT.get(), SCENARIO_JSON
			)
		) {
			scenario.run();
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
	}

	@AfterClass
	public static void tearDownClass() {
		try {
			HttpStorageMockTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Tear down class failed");
			e.printStackTrace(System.out);
		}
	}

	@Test
	public final void checkAllPerfSumFilesExist()
	throws Exception {
		for(int i = 0; i < 3; i ++) {
			Assert.assertTrue(
				RUN_ID_BASE + "_" + i,
				LogValidator.getPerfSumFile(RUN_ID_BASE + "_" + i).exists()
			);
		}
	}
}
