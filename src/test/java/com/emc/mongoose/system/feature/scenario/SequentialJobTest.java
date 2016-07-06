package com.emc.mongoose.system.feature.scenario;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
import com.emc.mongoose.system.base.HttpStorageMockTestBase;
import com.emc.mongoose.system.tools.BufferingOutputStream;
import com.emc.mongoose.system.tools.LogPatterns;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.system.tools.StdOutUtil;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by andrey on 09.06.16.
 */
public class SequentialJobTest
extends HttpStorageMockTestBase {

	private final static String RUN_ID = SequentialJobTest.class.getCanonicalName();
	private final static int LIMIT_COUNT = 1000;
	private final static String SCENARIO_JSON =
		"{" +
		"	\"type\" : \"sequential\",\n" +
		"	\"config\" : {\n" +
		"		\"load\" : {\n" +
		"			\"limit\" : {\n" +
		"				\"count\" : " + LIMIT_COUNT + "\n" +
		"			},\n" +
		"			\"threads\" : 50\n" +
		"		}\n" +
		"	},\n" +
		"	\"jobs\" : [\n" +
		"		{\n" +
		"			\"type\" : \"load\"\n" +
		"		}, {\n" +
		"			\"type\" : \"load\"\n" +
		"		}\n" +
		"	]\n" +
		"}\n";
	private final static Pattern START_MSG_PATTERN = Pattern.compile(
		LogPatterns.DATE_TIME_ISO8601.pattern() + "\\s+" +
			LogPatterns.LOG_LEVEL.pattern() + "\\s+" +
			LogPatterns.CLASS_NAME.pattern() + "\\s+" +
			LogPatterns.THREAD_NAME.pattern() + "\\s+Start\\sthe\\sjob\\s\"" +
			LogPatterns.CONSOLE_FULL_LOAD_NAME.pattern() + "\""
	);

	private static String STD_OUTPUT;

	@BeforeClass
	public static void setUpClass() {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		try {
			HttpStorageMockTestBase.setUpClass();
			Files.createDirectory(Paths.get(RUN_ID));
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
		try(final BufferingOutputStream stdOutStream = StdOutUtil.getStdOutBufferingStream()) {
			try(
				final Scenario
					scenario = new JsonScenario(BasicConfig.THREAD_CONTEXT.get(), SCENARIO_JSON)
			) {
				scenario.run();
			} catch(final CloneNotSupportedException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to build the scenario");
			}
			TimeUnit.SECONDS.sleep(1);
			STD_OUTPUT = stdOutStream.toString();
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
	}

	@AfterClass
	public static void tearDownClass() {
		try {
			HttpStorageMockTestBase.tearDownClass();
			LogValidator.removeLogDirectory(RUN_ID);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
	}

	@Test
	public void checkConsoleOutputThatJobsAreSequential()
	throws Exception {
		Matcher m = START_MSG_PATTERN.matcher(STD_OUTPUT);
		final DateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss,SSS");
		String ts;
		final List<Date> startDateTimes = new ArrayList<>();
		while(m.find()) {
			ts = m.group("dateTime");
			startDateTimes.add(dtFormat.parse(ts));
		}
		Assert.assertEquals(2, startDateTimes.size());
		m = LogPatterns.CONSOLE_METRICS_SUM.matcher(STD_OUTPUT);
		final List<Date> finishDateTimes = new ArrayList<>();
		while(m.find()) {
			ts = m.group("dateTime");
			finishDateTimes.add(dtFormat.parse(ts));
		}
		Assert.assertEquals(2, finishDateTimes.size());

		Assert.assertTrue(startDateTimes.get(0).getTime() < finishDateTimes.get(0).getTime());
		Assert.assertTrue(finishDateTimes.get(0).getTime() < startDateTimes.get(1).getTime());
		Assert.assertTrue(startDateTimes.get(1).getTime() < finishDateTimes.get(1).getTime());
	}
}
