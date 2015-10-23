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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
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
/**
 Created by andrey on 23.10.15.
 */
public class ReadContainersWithManyObjects
extends WSMockTestBase {
	//
	private static final int
		LIMIT_COUNT_OBJ = 100000,
		LIMIT_COUNT_CONTAINER = 100;
	//
	private static String RUN_ID_BASE = ReadContainersWithManyObjects.class.getCanonicalName();
	private static int countContainerCreated = 0;
	//
	@BeforeClass
	public static void setUpClass()
		throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID_BASE);
		System.setProperty(RunTimeConfig.KEY_ITEM_CLASS, "container");
		System.setProperty(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, Integer.toString(LIMIT_COUNT_OBJ));
		System.setProperty(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_COUNT_LIMIT, Integer.toString(LIMIT_COUNT_CONTAINER));
		System.setProperty(RunTimeConfig.KEY_DATA_SIZE, "1");
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
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID_BASE + "Write");
		rtConfig.set(RunTimeConfig.KEY_ITEM_CLASS, "data");
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
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID_BASE + "Read");
		rtConfig.set(RunTimeConfig.KEY_ITEM_CLASS, "container");
		rtConfig.set(RunTimeConfig.KEY_ITEM_SRC_FILE, containerListFile.toString());
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD, "read");
		rtConfig.set(RunTimeConfig.KEY_READ_CONNS, "25");
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, 0);
		RunTimeConfig.setContext(rtConfig);
		//
		new ScriptMockRunner().run();
		TimeUnit.SECONDS.sleep(1);
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
	public final void checkReadTotalByteRateIsNotZero()
	throws Exception {
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID_BASE + "Read");
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			//
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
				} else {
					LOG.info(Markers.MSG, nextRec);
				}
			}
		}

	}
}
