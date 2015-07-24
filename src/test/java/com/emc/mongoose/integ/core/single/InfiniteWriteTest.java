package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
//
import com.emc.mongoose.integ.tools.LogPatterns;
import com.emc.mongoose.integ.tools.ProcessManager;
import com.emc.mongoose.integ.tools.TestConstants;
//
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;

/**
 * Created by olga on 23.07.15.
 */
public class InfiniteWriteTest {
	//
	private static final long EXPECTED_RUN_TIME = 10000;
	private static long ACTUAL_RUN_TIME;
	private static Process process;

	@BeforeClass
	public static void before()
	throws Exception {

		// If tests run from the IDEA full logging file must be set
		final String fullLogConfFile = Paths
			.get(System.getProperty(TestConstants.USER_DIR_PROPERTY_NAME), Constants.DIR_CONF, TestConstants.LOG_FILE_NAME)
			.toString();
		System.setProperty(TestConstants.LOG_CONF_PROPERTY_KEY, fullLogConfFile);
		LogUtil.init();
		final Logger rootLogger = LogManager.getRootLogger();
		//Reload default properties
		final RunTimeConfig runTimeConfig = new RunTimeConfig();
		runTimeConfig.loadProperties();
		RunTimeConfig.setContext(runTimeConfig);
		//
		final ProcessBuilder processBuilder = new ProcessBuilder(
			"java", "-jar",
			RunTimeConfig.getContext().getRunName() + "-"+ RunTimeConfig.getContext().getRunVersion() +
			File.separator + "mongoose.jar"
		);
		processBuilder.directory(new File(System.getProperty("user.dir")));
		process = processBuilder.start();
		final int processID = getPid(process);
		Thread.sleep(EXPECTED_RUN_TIME);
		Runtime.getRuntime().exec(String.format("kill -SIGINT %d", processID));
	}

	@Test
	public void shouldWriteScenarioExitAfterSIGINT()
	throws Exception{
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		long startTime = 0, finishTime = 0;
		while ((line = br.readLine()) != null) {
			Matcher matcher = LogPatterns.DATE_TIME_ISO8601.matcher(line);
			if (matcher.find()) {
				finishTime = format.parse(matcher.group("time")).getTime();
				if (startTime == 0) {
					startTime = finishTime;
				}
			}
		}
		final long actualRunTime = finishTime - startTime;
		Assert.assertEquals("Mongoose run time is not equal expected time", EXPECTED_RUN_TIME, actualRunTime, 2000);
	}

	public static int getPid(Process process) {
		try {
			Class<?> ProcessImpl = process.getClass();
			Field field = ProcessImpl.getDeclaredField("pid");
			field.setAccessible(true);
			return field.getInt(process);
		} catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
			return -1;
		}
	}
}
