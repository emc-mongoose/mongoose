package com.emc.mongoose.system.feature.distributed;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.system.base.DistributedLoadBuilderTestBase;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.system.tools.TestConstants;
import com.emc.mongoose.run.scenario.runner.ScenarioRunner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 23.10.15.
 */
public class ReadBucketsWithManyObjectsDistributedTest
extends DistributedLoadBuilderTestBase {
	//
	private static final int
		LIMIT_COUNT_OBJ = 1000,
		LIMIT_COUNT_CONTAINER = 100;
	//
	private static String RUN_ID_BASE = ReadBucketsWithManyObjectsDistributedTest.class.getCanonicalName();
	private static int countContainerCreated = 0;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID_BASE);
		System.setProperty(AppConfig.KEY_ITEM_TYPE, "container");
		System.setProperty(AppConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, Integer.toString(LIMIT_COUNT_OBJ));
		System.setProperty(AppConfig.KEY_STORAGE_MOCK_CONTAINER_COUNT_LIMIT, Integer.toString(LIMIT_COUNT_CONTAINER));
		System.setProperty(AppConfig.KEY_ITEM_DATA_SIZE, "1");
		DistributedLoadBuilderTestBase.setUpClass();
		final AppConfig rtConfig = BasicConfig.THREAD_CONTEXT.get();
		rtConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_COUNT, Integer.toString(LIMIT_COUNT_CONTAINER));
		rtConfig.setProperty(AppConfig.KEY_LOAD_TYPE, TestConstants.LOAD_CREATE);
		rtConfig.setProperty(AppConfig.KEY_LOAD_THREADS, "10");
		//
		final Logger logger = LogManager.getLogger();
		logger.info(Markers.MSG, BasicConfig.THREAD_CONTEXT.get().toString());
		//
		new ScenarioRunner(rtConfig).run();
		TimeUnit.SECONDS.sleep(10);
		//
		RunIdFileManager.flushAll();
		//
		final File containerListFile = LogValidator.getItemsListFile(RUN_ID_BASE);
		Assert.assertTrue("items list file doesn't exist", containerListFile.exists());
		//
		String nextContainer;
		rtConfig.setRunId(RUN_ID_BASE + "Write");
		rtConfig.setProperty(AppConfig.KEY_ITEM_TYPE, "data");
		rtConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT_OBJ);
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
					rtConfig.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, nextContainer);
					new ScenarioRunner(rtConfig).run();
					TimeUnit.SECONDS.sleep(1);
				}
			} while(true);
		}
		//
		rtConfig.setRunId(RUN_ID_BASE + "Read");
		rtConfig.setProperty(AppConfig.KEY_ITEM_TYPE, "container");
		rtConfig.setProperty(AppConfig.KEY_ITEM_SRC_FILE, containerListFile.toString());
		rtConfig.setProperty(AppConfig.KEY_LOAD_TYPE, "read");
		rtConfig.setProperty(AppConfig.KEY_LOAD_THREADS, "10");
		rtConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_COUNT, 0);
		//
		new ScenarioRunner(rtConfig).run();
		TimeUnit.SECONDS.sleep(10);
		//
		RunIdFileManager.flushAll();
	}
	//
	@AfterClass
	public  static void tearDownClass()
		throws Exception {
		DistributedLoadBuilderTestBase.tearDownClass();
		System.setProperty(AppConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, "1000000");
		System.setProperty(AppConfig.KEY_STORAGE_MOCK_CONTAINER_COUNT_LIMIT, "1000000");
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
					final float
						containerCount = Long.parseLong(nextRec.get(7)),
						avgRate = Float.parseFloat(nextRec.get(21)),
						avgByteRate = Float.parseFloat(nextRec.get(23));
					Assert.assertEquals(
						"Read container count should mismatch",
						countContainerCreated, containerCount, 1
					);
					Assert.assertTrue("Container read rate should be > 0", avgRate > 0);
					Assert.assertTrue("Container read byte rate should be > 0", avgByteRate > 0);
				}
			}
		}
	}
}
