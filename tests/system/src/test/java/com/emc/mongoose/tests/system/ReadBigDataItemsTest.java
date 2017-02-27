package com.emc.mongoose.tests.system;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.tests.system.util.PortListener;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;
import static com.emc.mongoose.model.storage.StorageDriver.BUFF_SIZE_MAX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 Created by kurila on 30.01.17.
 Covered use cases:
 * 2.1.1.1.4. Big Data Items (100MB)
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 4.2. Small Concurrency Level (10)
 * 6.1. Load Job Naming
 * 6.2.4. Limit By Size
 * 6.2.6. Limit By End of Items Input
 * 7.1. Periodic Reporting
 * 7.4. I/O Traces Reporting
 * 8.3.2. Read - Enabled Validation
 * 9.2. Default Scenario
 * 10.1.4. Two Local Separate Storage Driver Services (at different ports)
 * 10.4.4. I/O Buffer Size Adjustment for Optimal Performance
 */
public class ReadBigDataItemsTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final SizeInBytes ITEM_DATA_SIZE = new SizeInBytes("100MB");
	private static final String ITEM_OUTPUT_FILE = ReadBigDataItemsTest.class.getSimpleName() + ".csv";
	private static final int LOAD_CONCURRENCY = 10;
	private static final SizeInBytes LOAD_LIMIT_SIZE = new SizeInBytes("100GB");
	private static final long EXPECTED_COUNT = LOAD_LIMIT_SIZE.get() / ITEM_DATA_SIZE.get();
	
	private static String STD_OUTPUT = null;
	private static int ACTUAL_CONCURRENCY = 0;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_JOB_NAME, ReadBigDataItemsTest.class.getSimpleName());
		CONFIG_ARGS.add("--item-data-size=" + ITEM_DATA_SIZE.toString());
		try {
			Files.delete(Paths.get(ITEM_OUTPUT_FILE));
		} catch(final NoSuchFileException ignored) {
		}
		CONFIG_ARGS.add("--item-output-file=" + ITEM_OUTPUT_FILE);
		CONFIG_ARGS.add("--load-limit-size=" + LOAD_LIMIT_SIZE.toString());
		CONFIG_ARGS.add("--load-concurrency=" + LOAD_CONCURRENCY);
		HttpStorageDistributedScenarioTestBase.setUpClass();
		SCENARIO.run();
		
		// reinit
		SCENARIO.close();
		LoadJobLogFileManager.closeAll(JOB_NAME);
		JOB_NAME = ReadBigDataItemsTest.class.getSimpleName() + "_";
		FileUtils.deleteDirectory(Paths.get(PathUtil.getBaseDir(), "log", JOB_NAME).toFile());
		ThreadContext.put(KEY_JOB_NAME, JOB_NAME);
		LogUtil.init();
		LOG = LogManager.getLogger();
		CONFIG_ARGS.add("--item-data-verify");
		CONFIG_ARGS.add("--item-input-file=" + ITEM_OUTPUT_FILE);
		CONFIG_ARGS.add("--load-type=read");
		CONFIG.apply(
			CliArgParser.parseArgs(
				CONFIG.getAliasingConfig(), CONFIG_ARGS.toArray(new String[CONFIG_ARGS.size()])
			)
		);
		CONFIG.getItemConfig().getOutputConfig().setFile(null);
		CONFIG.getLoadConfig().getLimitConfig().setCount(0);
		CONFIG.getLoadConfig().getJobConfig().setName(JOB_NAME);
		SCENARIO = new JsonScenario(CONFIG, DEFAULT_SCENARIO_PATH.toFile());
		
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
		TimeUnit.SECONDS.sleep(15); // warmup
		final int startPort = CONFIG.getStorageConfig().getNetConfig().getNodeConfig().getPort();
		for(int i = 0; i < STORAGE_NODE_COUNT; i ++) {
			ACTUAL_CONCURRENCY += PortListener
				.getCountConnectionsOnPort("127.0.0.1:" + (startPort + i));
		}
		TimeUnit.MINUTES.timedJoin(runner, 5);
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	
	@Test
	public void testActiveConnectionsCount()
	throws Exception {
		assertEquals(STORAGE_DRIVERS_COUNT * LOAD_CONCURRENCY, ACTUAL_CONCURRENCY);
	}
	
	@Test public void testMetricsLogFile()
	throws Exception {
		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, EXPECTED_COUNT, 0,
			CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test @Ignore
	public void testTotalMetricsLogFile()
	throws Exception {
		testTotalMetricsLogRecords(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE, EXPECTED_COUNT, 0
		);
	}
	
	@Test public void testMetricsStdout()
	throws Exception {
		testMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, LOAD_CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getLoadConfig().getMetricsConfig().getPeriod()
		);
	}
	
	@Test public void testIoTraceLogFile()
	throws Exception {
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(EXPECTED_COUNT, ioTraceRecords.size(), EXPECTED_COUNT / 5);
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), ITEM_DATA_SIZE);
		}
	}
	
	@Test public void testIoBufferSizeAdjustment()
	throws Exception {
		String msg = "Adjust input buffer size: " + SizeInBytes.formatFixedSize(BUFF_SIZE_MAX);
		int k;
		for(int i = 0; i < STORAGE_DRIVERS_COUNT; i ++) {
			k = STD_OUTPUT.indexOf(msg);
			if(k > -1) {
				msg = STD_OUTPUT.substring(k + msg.length());
			} else {
				fail("Expected the message to occur " + STORAGE_DRIVERS_COUNT + " times, but got " + i);
			}
		}
	}
}
