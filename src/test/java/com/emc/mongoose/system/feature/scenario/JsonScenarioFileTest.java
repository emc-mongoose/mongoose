package com.emc.mongoose.system.feature.scenario;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
import com.emc.mongoose.system.base.WSMockTestBase;
import com.emc.mongoose.system.tools.BufferingOutputStream;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.system.tools.StdOutUtil;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
/**
 Created by andrey on 08.06.16.
 */
public class JsonScenarioFileTest
extends WSMockTestBase {

	private final static String RUN_ID =JsonScenarioFileTest.class.getCanonicalName();
	private final static int EXPECTED_DURATION_SECONDS = 30;
	private final static String SCENARIO_JSON =
		"{\n" +
		"	\"type\" : \"load\",\n" +
		"	\"config\" : {\n" +
		"		\"item\" : {\n" +
		"			\"dst\" : {\n" +
		"				\"file\" : \"" + RUN_ID + File.separator + "items.csv\"\n" +
		"			}\n" +
		"		},\n" +
		"		\"load\" : {\n" +
		"			\"limit\" : {\n" +
		"				\"time\" : \"" + EXPECTED_DURATION_SECONDS + "s\"\n" +
		"			}\n" +
		"		}\n" +
		"	}\n" +
		"}\n";

	private static long ACTUAL_DURATION_MILLISECONDS = 0;

	@BeforeClass
	public static void setUpClass() {
		final Path tgtDirPath = Paths.get(RUN_ID);
		if(!Files.exists(tgtDirPath)) {
			try {
				Files.createDirectory(Paths.get(RUN_ID));
			} catch(final IOException e) {
				e.printStackTrace(System.out);
				Assert.fail();
			}
		}
		final File scenarioFile = new File(RUN_ID + File.separator + "testScenario.json");
		try(
			final BufferedWriter out = Files.newBufferedWriter(
				scenarioFile.toPath(), UTF_8, CREATE, WRITE, TRUNCATE_EXISTING
			)
		) {
			out.write(SCENARIO_JSON);
		} catch(final IOException e) {
			e.printStackTrace(System.out);
			Assert.fail();
		}
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		try {
			WSMockTestBase.setUpClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
		try(
			final Scenario scenario = new JsonScenario(
				BasicConfig.THREAD_CONTEXT.get(), scenarioFile
			)
		) {
			ACTUAL_DURATION_MILLISECONDS = System.currentTimeMillis();
			scenario.run();
			ACTUAL_DURATION_MILLISECONDS = System.currentTimeMillis() -
				ACTUAL_DURATION_MILLISECONDS;
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
	}

	@AfterClass
	public static void tearDownClass() {
		try {
			WSMockTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
		final File d = new File(RUN_ID);
		for(final File f : d.listFiles()) {
			f.delete();
		}
		d.delete();
	}

	@Test
	public void checkDurationDiffersNoMoreThan1Sec()
	throws Exception {
		Assert.assertEquals(
			EXPECTED_DURATION_SECONDS,
			TimeUnit.MILLISECONDS.toSeconds(ACTUAL_DURATION_MILLISECONDS), 1
		);
	}

	@Test
	public void checkItemsDstFile()
	throws Exception {
		try(
			final BufferedReader in = Files.newBufferedReader(
				Paths.get(RUN_ID, "items.csv"), StandardCharsets.UTF_8
			)
		) {
			LogValidator.assertCorrectItemsCsv(in);
		}
	}
}
