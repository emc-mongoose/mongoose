package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
//
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.tools.LogPatterns;
//
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;

/**
 * Created by olga on 23.07.15.
 */
public class InfiniteWriteTest
extends WSMockTestBase {
	//
	private static final long EXPECTED_RUN_TIME = 10000;
	private static Process PROCESS;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, InfiniteWriteTest.class.getCanonicalName());
		WSMockTestBase.setUpClass();
		//
		final String runName = RunTimeConfig.getContext().getRunName();
		final String runVersion = RunTimeConfig.getContext().getRunVersion();

		final String mongooseTgzPath = Paths.get(new File(RunTimeConfig.DIR_ROOT).getParent(),
				Constants.DIR_DIST).resolve(runName + "-" + runVersion + ".tgz").toString();

		if (!new File(mongooseTgzPath).exists()) {
			throw new FileNotFoundException();
		}

		final ProcessBuilder builder = new ProcessBuilder();
		builder.command("tar", "xvf", mongooseTgzPath);
		builder.directory(new File(System.getProperty("user.dir")));
		PROCESS = builder.start();
		PROCESS.waitFor();
		//
		final ProcessBuilder processBuilder = new ProcessBuilder(
			"java", "-jar", runName + "-" + runVersion +
			File.separator + runName + ".jar"
		);
		processBuilder.directory(new File(System.getProperty("user.dir")));
		PROCESS = processBuilder.start();
		final int processID = getPid(PROCESS);
		Thread.sleep(EXPECTED_RUN_TIME);
		Runtime.getRuntime().exec(String.format("kill -SIGINT %d", processID));
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
	}

	@Test
	public void shouldWriteScenarioExitAfterSIGINT()
	throws Exception{
		try (
			final BufferedReader bufReader = new BufferedReader(
				new InputStreamReader(PROCESS.getInputStream())
			)
		) {
			String line;
			final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
			long startTime = 0, finishTime = 0;
			while ((line = bufReader.readLine()) != null) {
				Matcher matcher = LogPatterns.DATE_TIME_ISO8601.matcher(line);
				if (matcher.find()) {
					finishTime = format.parse(matcher.group("time")).getTime();
					if (startTime == 0) {
						startTime = finishTime;
					}
				}
			}
			final long actualRunTime = finishTime - startTime;
			Assert.assertEquals(
				"Mongoose run time is not equal expected time",
				EXPECTED_RUN_TIME, actualRunTime, 5500
			);
		}
	}

	private static int getPid(Process process) {
		try {
			Class<?> ProcessImpl = process.getClass();
			Field field = ProcessImpl.getDeclaredField("pid");
			field.setAccessible(true);
			return field.getInt(process);
		} catch (final Exception e) {
			return -1;
		}
	}
}
