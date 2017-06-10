package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

/**
 Created by andrey on 10.06.17.
 */
public class FileStorageMetricsThresholdTest
extends EnvConfiguredScenarioTestBase {

	private static String ITEM_OUTPUT_PATH;
	private static String STD_OUTPUT;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("atmos", "s3", "swift"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(1, 1000));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes(0), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = FileStorageMetricsThresholdTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "FileStorageMetricsThreshold.json"
		);
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_STEP_NAME, STEP_NAME);
		EnvConfiguredScenarioTestBase.setUpClass();
		if(EXCLUDE_FLAG) {
			return;
		}
		ITEM_OUTPUT_PATH = Paths.get(
			Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
		).toString();
		CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		SCENARIO.run();
		LoadJobLogFileManager.flushAll();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
		TimeUnit.SECONDS.sleep(10);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(!EXCLUDE_FLAG) {
			try {
				DirWithManyFilesDeleter.deleteExternal(ITEM_OUTPUT_PATH);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}
}
