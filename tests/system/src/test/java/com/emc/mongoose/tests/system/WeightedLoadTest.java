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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertTrue;

/**
 Created by kurila on 16.02.17.
 */
public class WeightedLoadTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final Path SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, "mixed", "weighted.json"
	);
	private static final SizeInBytes EXPECTED_ITEM_DATA_SIZE = new SizeInBytes("1-1KB");
	private static final int EXPECTED_CONCURRENCY = 200;
	private static final String ITEM_OUTPUT_FILE = "weighted-load.csv";
	
	private static String STD_OUTPUT;
	private static boolean FINISHED_IN_TIME;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		JOB_NAME = WeightedLoadTest.class.getSimpleName();
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE));
		} catch(final Exception ignored) {
		}
		ThreadContext.put(KEY_JOB_NAME, JOB_NAME);
		CONFIG_ARGS.add("--scenario-file=" + SCENARIO_PATH.toString());
		CONFIG_ARGS.add("--item-data-verify=true");
		HttpStorageDistributedScenarioTestBase.setUpClass();
		final Thread runner = new Thread(
			() -> {
				try {
					STD_OUT_STREAM.startRecording();
					SCENARIO.run();
					STD_OUTPUT = STD_OUT_STREAM.stopRecording();
				} catch(final Throwable t) {
					LogUtil.exception(LOG, Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.MINUTES.timedJoin(runner, 2);
		FINISHED_IN_TIME = !runner.isAlive();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	
	@Test
	public final void testFinishedInTime() {
		assertTrue(FINISHED_IN_TIME);
	}
}
