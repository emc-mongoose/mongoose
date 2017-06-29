package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.appenders.TestStepIdLogFileManager;
import static com.emc.mongoose.common.Constants.KEY_STEP_ID;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 12.06.17.
 */
public class ReadCustomContentVerificationFailTest
extends EnvConfiguredScenarioTestBase {

	private static String ITEM_OUTPUT_PATH;

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(10, 1000));
		EXCLUDE_PARAMS.put(
			KEY_ENV_ITEM_DATA_SIZE,
			Arrays.asList(new SizeInBytes(0), new SizeInBytes("100MB"), new SizeInBytes("10GB"))
		);
		STEP_NAME = ReadCustomContentVerificationFailTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "ReadVerificationFail.json"
		);
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_STEP_ID, STEP_NAME);
		CONFIG_ARGS.add(
			"--item-data-content-file=" + PathUtil.getBaseDir() + "/config/content/textexample"
		);
		CONFIG_ARGS.add("--storage-net-http-namespace=ns1");
		EnvConfiguredScenarioTestBase.setUpClass();
		if(SKIP_FLAG) {
			return;
		}
		if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
			ITEM_OUTPUT_PATH = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), STEP_NAME
			).toString();
			CONFIG.getItemConfig().getOutputConfig().setPath(ITEM_OUTPUT_PATH);
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		SCENARIO.run();
		TestStepIdLogFileManager.flushAll();
		TimeUnit.SECONDS.sleep(5);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(! SKIP_FLAG) {
			if(STORAGE_DRIVER_TYPE.equals(STORAGE_TYPE_FS)) {
				try {
					DirWithManyFilesDeleter.deleteExternal(ITEM_OUTPUT_PATH);
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		CSVRecord csvRecord;
		for(int i = 0; i < ioTraceRecords.size(); i ++) {
			csvRecord = ioTraceRecords.get(i);
			assertEquals(
				"Record #" + i + ": unexpected operation type " + csvRecord.get("IoTypeCode"),
				IoType.READ,
				IoType.values()[Integer.parseInt(csvRecord.get("IoTypeCode"))]
			);
			assertEquals(
				"Record #" + i + ": unexpected status code " + csvRecord.get("StatusCode"),
				IoTask.Status.RESP_FAIL_CORRUPT,
				IoTask.Status.values()[Integer.parseInt(csvRecord.get("StatusCode"))]
			);
		}
	}
}
