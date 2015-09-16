package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
//
import com.emc.mongoose.common.log.Markers;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * Created by olga on 23.07.15.
 */
public class InfiniteWriteTest
extends WSMockTestBase {
	//
	private static final long RUN_TIME_OUT_SEC = 100;
	private static Process PROCESS;
	private static int PID;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, InfiniteWriteTest.class.getCanonicalName());
		WSMockTestBase.setUpClass();
		//
		final String
			gooseName = RunTimeConfig.getContext().getRunName(),
			gooseVersion = RunTimeConfig.getContext().getRunVersion();
		final File
			gooseTgzFile = Paths.get("build", "dist", gooseName + "-" + gooseVersion + ".tgz").toFile(),
			gooseJarFile = Paths.get(gooseName + "-" + gooseVersion, gooseName + ".jar").toFile();
		if(!gooseTgzFile.exists()) {
			Assert.fail("Mongoose tgz file not found @ " + gooseTgzFile.getAbsolutePath());
		}
		final ProcessBuilder processBuilder = new ProcessBuilder();
		PROCESS = processBuilder.command("tar", "xvf", gooseTgzFile.getPath()).start();
		PROCESS.waitFor();
		if(!gooseJarFile.exists()) {
			Assert.fail("Mongoose jar file not found @ " + gooseJarFile.getAbsolutePath());
		}
		processBuilder.command("java", "-jar", gooseJarFile.getPath());
		processBuilder.directory(new File(System.getProperty("user.dir")));
		PROCESS = processBuilder.start();
		PID = getPid(PROCESS);
		LOG.info(Markers.MSG, "Launched separate goose process w/ PID #{}", PID);
		TimeUnit.SECONDS.sleep(RUN_TIME_OUT_SEC);
		String cmdKill = String.format("kill -SIGINT %d", PID);
		Runtime.getRuntime().exec(cmdKill);
		LOG.info(Markers.MSG, "Executed the command: \"{}\"", cmdKill);
		cmdKill = String.format("kill -SIGTERM %d", PID);
		Runtime.getRuntime().exec(cmdKill);
		LOG.info(Markers.MSG, "Executed the command: \"{}\"", cmdKill);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
	}

	@Test
	public void shouldHaveCorrectPID() {
		Assert.assertTrue("Invalid pid: " + PID, PID > 1);
	}

	@Test
	public void shouldWriteScenarioExitAfterSIGINT()
	throws Exception{
		try(
			final BufferedReader bufReader = new BufferedReader(
				new InputStreamReader(PROCESS.getInputStream())
			)
		) {
			String line;
			Matcher matcher;
			final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
			long startTime = 0, finishTime = 0;
			while((line = bufReader.readLine()) != null) {
				matcher = LogPatterns.DATE_TIME_ISO8601.matcher(line);
				if(matcher.find()) {
					finishTime = format.parse(matcher.group("time")).getTime();
					if(startTime == 0) {
						startTime = finishTime;
					}
				}
			}
			final long actualRunTime = finishTime - startTime;
			Assert.assertEquals(
				"Mongoose run time is not equal expected time",
				RUN_TIME_OUT_SEC, actualRunTime / 1000, 10
			);
		}
	}

	private static int getPid(Process process) {
		try {
			final Class<?> processImplCls = process.getClass();
			final Field field = processImplCls.getDeclaredField("pid");
			field.setAccessible(true);
			return field.getInt(process);
		} catch (final Exception e) {
			return -1;
		}
	}
}
