package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static org.junit.Assert.assertTrue;

/**
 Created by kurila on 21.04.17.
 */
public class TlsTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final SizeInBytes ITEM_DATA_SIZE = new SizeInBytes("10B");
	private static final String ITEM_OUTPUT_FILE = TlsTest.class.getSimpleName() + ".csv";
	private static final int LOAD_LIMIT_COUNT = 100;
	private static final int LOAD_CONCURRENCY = 10;
	
	private static boolean FINISHED_IN_TIME = true;
	private static String STD_OUTPUT = null;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = TlsTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		CONFIG_ARGS.add("--item-data-size=" + ITEM_DATA_SIZE.toString());
		CONFIG_ARGS.add("--item-output-file=" + ITEM_OUTPUT_FILE);
		CONFIG_ARGS.add("--test-step-limit-count=" + LOAD_LIMIT_COUNT);
		CONFIG_ARGS.add("--storage-driver-concurrency=" + LOAD_CONCURRENCY);
		CONFIG_ARGS.add("--storage-net-ssl=" + Boolean.TRUE.toString());
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE));
		} catch(final IOException ignored) {
		}
		HttpStorageDistributedScenarioTestBase.setUpClass();
		final Thread runner = new Thread(
			() -> {
				try {
					SCENARIO.run();
				} catch(final Throwable t) {
					LogUtil.exception(LOG, Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		STD_OUT_STREAM.startRecording();
		runner.start();
		TimeUnit.SECONDS.timedJoin(runner, 1000);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
		STD_OUTPUT = STD_OUT_STREAM.stopRecording();
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	
	@Test
	public void testFinishedInTime()
	throws Exception {
		assertTrue(FINISHED_IN_TIME);
	}
}
