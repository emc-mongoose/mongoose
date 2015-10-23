package com.emc.mongoose.integ.core.single;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.integ.base.WSMockTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.run.scenario.runner.ScriptMockRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by andrey on 23.10.15.
 */
public class ReadContainersWithManyObjects
extends WSMockTestBase {
	//
	private static BufferingOutputStream STD_OUTPUT_STREAM;
	private static final int
		LIMIT_COUNT_OBJ = 200000,
		LIMIT_COUNT_CONTAINER = 50;
	//
	private static String RUN_ID_BASE = ReadContainersWithManyObjects.class.getCanonicalName();
	private static int countContainerCreated = 0;
	//
	@BeforeClass
	public static void setUpClass()
		throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID_BASE);
		System.setProperty(RunTimeConfig.KEY_LOAD_ITEM_CLASS, "container");
		System.setProperty(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, Integer.toString(LIMIT_COUNT_OBJ));
		System.setProperty(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_COUNT_LIMIT, Integer.toString(LIMIT_COUNT_CONTAINER));
		System.setProperty(RunTimeConfig.KEY_DATA_SIZE, "1KB");
		WSMockTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT_CONTAINER));
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, TestConstants.LOAD_CREATE);
		rtConfig.set(RunTimeConfig.KEY_CREATE_CONNS, "25");
		RunTimeConfig.setContext(rtConfig);
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//
		new ScriptMockRunner().run();
		TimeUnit.SECONDS.sleep(1);
		//
		RunIdFileManager.flushAll();
		//
		final File containerListFile = LogValidator.getItemsListFile(RUN_ID_BASE);
		Assert.assertTrue("items list file doesn't exist", containerListFile.exists());
		//
		String nextContainer, nextRunId;
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, "Write");
		rtConfig.set(RunTimeConfig.KEY_LOAD_ITEM_CLASS, "data");
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT_OBJ);
		RunTimeConfig.setContext(rtConfig);
		try(
			final BufferedReader
				in = Files.newBufferedReader(containerListFile.toPath(), StandardCharsets.UTF_8)
		) {
			do {
				nextContainer = in.readLine();
				if(nextContainer == null) {
					break;
				} else {
					countContainerCreated++;
					rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, nextContainer);
					RunTimeConfig.setContext(rtConfig);
					new ScriptMockRunner().run();
					TimeUnit.SECONDS.sleep(1);
				}
			} while(true);
		}
		//
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, "Read");
		rtConfig.set(RunTimeConfig.KEY_LOAD_ITEM_CLASS, "container");
		rtConfig.set(RunTimeConfig.KEY_DATA_SRC_FPATH, containerListFile.toString());
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, "read");
		rtConfig.set(RunTimeConfig.KEY_READ_CONNS, "25");
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, 0);
		RunTimeConfig.setContext(rtConfig);
		//
		try(
			final BufferingOutputStream
				outStream = StdOutInterceptorTestSuite.getStdOutBufferingStream()
		) {
			new ScriptMockRunner().run();
			TimeUnit.SECONDS.sleep(1);
			STD_OUTPUT_STREAM = outStream;
		}
		//
		RunIdFileManager.flushAll();
	}
	//
	@AfterClass
	public  static void tearDownClass()
		throws Exception {
		WSMockTestBase.tearDownClass();
		System.setProperty(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, "1000000");
		System.setProperty(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_COUNT_LIMIT, "1000000");
	}
	//
	@Test
	public final void checkCreatedContainerCount()
		throws Exception {
		Assert.assertEquals(LIMIT_COUNT_CONTAINER, countContainerCreated);
	}
	//
	@Test
	public final void checkThatReadByteRateIsNotZero() {
		final String consoleOutput = STD_OUTPUT_STREAM.toString();
		final Pattern p = Pattern.compile("Bucket \"[a-z0-9]+\" already exists");
		final Matcher m = p.matcher(consoleOutput);
		int countMatch = 0;
		while(m.find()) {
			countMatch ++;
		}
		Assert.assertEquals(LIMIT_COUNT_CONTAINER, countMatch);
	}
}
