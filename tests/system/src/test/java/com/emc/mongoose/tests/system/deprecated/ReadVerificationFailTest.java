package com.emc.mongoose.tests.system.deprecated;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.Status;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.deprecated.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.CloseableThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static org.junit.Assert.assertEquals;

/**
 Created by kurila on 30.05.17.
 * 1.1. Configuration Syntax
 * 1.2. CLI Arguments Aliasing
 * 2.1.1.1.2. Small Data Items (1B-100KB)
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 3.2. Payload From the External File
 * 4.3. Medium Concurrency Level (11-100)
 * 6.2.2. Limit Step by Processed Item Count
 * 6.2.6. Limit Step by End of Items Input
 * 7.4. I/O Traces Reporting
 * 8.2.1. Create New Items
 * 8.3.2. Read With Enabled Validation
 * 8.4.2.1. Single Random Range Update
 * 9.2. Default Scenario
 * 10.1.2. Many Local Separate Storage Driver Services (at different ports)
 */

public final class ReadVerificationFailTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final SizeInBytes EXPECTED_ITEM_DATA_SIZE = new SizeInBytes("10KB");
	private static final int EXPECTED_CONCURRENCY = 25;
	private static final long EXPECTED_COUNT = 100000;
	private static final String ITEM_OUTPUT_FILE_0 = ReadVerificationFailTest.class.getSimpleName() +
		"0.csv";
	private static final String ITEM_OUTPUT_FILE_1 = ReadVerificationFailTest.class.getSimpleName() +
		"1.csv";
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE_0));
		} catch(final IOException ignored) {
		}
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE_1));
		} catch(final IOException ignored) {
		}
		
		STEP_NAME = ReadVerificationFailTest.class.getSimpleName() + "0";
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_STEP_NAME, STEP_NAME)
		) {
			FileUtils.deleteDirectory(Paths.get(PathUtil.getBaseDir(), "log", STEP_NAME).toFile());
			CONFIG_ARGS.add("--item-data-content-file=" + PathUtil.getBaseDir() + "/config/content/zerobytes");
			CONFIG_ARGS.add("--item-data-size=" + EXPECTED_ITEM_DATA_SIZE.toString());
			CONFIG_ARGS.add("--item-output-file=" + ITEM_OUTPUT_FILE_0);
			CONFIG_ARGS.add("--storage-driver-concurrency=" + EXPECTED_CONCURRENCY);
			CONFIG_ARGS.add("--test-step-limit-count=" + EXPECTED_COUNT);
			HttpStorageDistributedScenarioTestBase.setUpClass();
			SCENARIO.run();
		}
		
		STEP_NAME = ReadVerificationFailTest.class.getSimpleName() + "1";
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_STEP_NAME, STEP_NAME)
		) {
			FileUtils.deleteDirectory(Paths.get(PathUtil.getBaseDir(), "log", STEP_NAME).toFile());
			CONFIG_ARGS.clear();
			CONFIG_ARGS.add("--update");
			CONFIG_ARGS.add("--item-data-content-file=" + PathUtil.getBaseDir() + "/config/content/zerobytes");
			CONFIG_ARGS.add("--item-data-ranges-random=3");
			CONFIG_ARGS.add("--item-input-file=" + ITEM_OUTPUT_FILE_0);
			CONFIG_ARGS.add("--item-output-file=" + ITEM_OUTPUT_FILE_1);
			CONFIG_ARGS.add("--storage-driver-concurrency=" + EXPECTED_CONCURRENCY);
			CONFIG = ConfigParser.loadDefaultConfig();
			CONFIG.apply(
				CliArgParser.parseArgs(
					CONFIG.getAliasingConfig(), CONFIG_ARGS.toArray(new String[CONFIG_ARGS.size()])
				)
			);
			CONFIG.getTestConfig().getStepConfig().setName(STEP_NAME);
			CONFIG.getTestConfig().getStepConfig().getLimitConfig().setCount(0);
			SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
			SCENARIO.run();
		}
		
		STEP_NAME = ReadVerificationFailTest.class.getSimpleName() + "2";
		try(
			final CloseableThreadContext.Instance logCtx = CloseableThreadContext
				.put(KEY_STEP_NAME, STEP_NAME)
		) {
			FileUtils.deleteDirectory(Paths.get(PathUtil.getBaseDir(), "log", STEP_NAME).toFile());
			CONFIG_ARGS.clear();
			CONFIG_ARGS.add("--read");
			CONFIG_ARGS.add("--item-data-content-file=" + PathUtil.getBaseDir() + "/config/content/zerobytes");
			CONFIG_ARGS.add("--item-data-verify=" + Boolean.TRUE.toString());
			CONFIG_ARGS.add("--item-input-file=" + ITEM_OUTPUT_FILE_0);
			CONFIG_ARGS.add("--storage-driver-concurrency=" + EXPECTED_CONCURRENCY);
			CONFIG = ConfigParser.loadDefaultConfig();
			CONFIG.apply(
				CliArgParser.parseArgs(
					CONFIG.getAliasingConfig(), CONFIG_ARGS.toArray(new String[CONFIG_ARGS.size()])
				)
			);
			CONFIG.getTestConfig().getStepConfig().setName(STEP_NAME);
			SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
			SCENARIO.run();
		}
		LoadJobLogFileManager.flush(STEP_NAME);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	

	public void testIoTraceLogFile()
	throws Exception {
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(
			"There should be " + EXPECTED_COUNT + " records in the I/O trace log file, but got: " +
				ioTraceRecords.size(),
			EXPECTED_COUNT, ioTraceRecords.size()
		);
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
				Status.RESP_FAIL_CORRUPT,
				IoTask.Status.values()[Integer.parseInt(csvRecord.get("StatusCode"))]
			);
		}
	}
}
