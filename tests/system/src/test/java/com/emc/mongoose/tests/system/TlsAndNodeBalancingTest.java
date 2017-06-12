package com.emc.mongoose.tests.system;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import org.apache.commons.csv.CSVRecord;

import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 12.06.17.
 */
@Ignore
public class TlsAndNodeBalancingTest
extends EnvConfiguredScenarioTestBase {

	static {
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("fs"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_COUNT, Arrays.asList(2));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, Arrays.asList(100, 1000));
		STEP_NAME = TlsAndNodeBalancingTest.class.getSimpleName();
		SCENARIO_PATH = Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "ReadUsingInputPath.json");
	}

	private static String STD_OUTPUT;
	private static long ACTUAL_TEST_TIME_MILLISECONDS;

	private static final int EXPECTED_TEST_TIME_MINUTES = 3;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ThreadContext.put(KEY_STEP_NAME, STEP_NAME);
		HTTP_STORAGE_NODE_COUNT = 4;
		CONFIG_ARGS.add("--storage-mock-node=true");
		CONFIG_ARGS.add("--storage-net-ssl=true");
		EnvConfiguredScenarioTestBase.setUpClass();
		if(EXCLUDE_FLAG) {
			return;
		}
		SCENARIO = new JsonScenario(CONFIG, SCENARIO_PATH.toFile());
		STD_OUT_STREAM.startRecording();
		ACTUAL_TEST_TIME_MILLISECONDS = System.currentTimeMillis();
		SCENARIO.run();
		ACTUAL_TEST_TIME_MILLISECONDS = System.currentTimeMillis() - ACTUAL_TEST_TIME_MILLISECONDS;
		LoadJobLogFileManager.flushAll();
		STD_OUTPUT = STD_OUT_STREAM.stopRecordingAndGet();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Test
	public void testFinishedInTime() {
		assumeFalse(EXCLUDE_FLAG);
		assertEquals(
			TimeUnit.MINUTES.toMillis(EXPECTED_TEST_TIME_MINUTES) + 10000,
			ACTUAL_TEST_TIME_MILLISECONDS,
			TimeUnit.MINUTES.toMillis(EXPECTED_TEST_TIME_MINUTES)
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 0
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(1), IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT,
			ITEM_DATA_SIZE, 0, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, CONCURRENCY, STORAGE_DRIVERS_COUNT, ITEM_DATA_SIZE,
			CONFIG.getTestConfig().getStepConfig().getMetricsConfig().getPeriod()
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		String storageNodeAddr;
		final Object2IntMap<String> nodeFreq = new Object2IntOpenHashMap<>();
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), ITEM_DATA_SIZE);
			storageNodeAddr = ioTraceRecord.get("StorageNode");
			if(nodeFreq.containsKey(storageNodeAddr)) {
				nodeFreq.put(storageNodeAddr, nodeFreq.getInt(storageNodeAddr) + 1);
			} else {
				nodeFreq.put(storageNodeAddr, 1);
			}
		}

		final ObjectSet<String> storageNodeSet = nodeFreq.keySet();
		assertTrue(storageNodeSet.size() > 0);
		final double expectedAvgPerNode = ((double) ioTraceRecords.size()) / storageNodeSet.size();
		for(final String nextNode : storageNodeSet) {
			assertEquals(
				"Actual node " + nextNode + " record count: " + nodeFreq.getInt(nextNode) +
					", expected: " + expectedAvgPerNode,
				expectedAvgPerNode, nodeFreq.getInt(nextNode), expectedAvgPerNode / 1000
			);
		}
	}

	@Test
	public void testTlsEnableLogged()
	throws Exception {
		assumeFalse(EXCLUDE_FLAG);
		final List<String> msgLogLines = getMessageLogLines();
		int msgCount = 0;
		for(final String msgLogLine : msgLogLines) {
			if(msgLogLine.contains(STEP_NAME + ": SSL/TLS is enabled for the channel")) {
				msgCount ++;
			}
		}
		// 3 steps + additional bucket checking/creating connections
		Assert.assertTrue(3 * STORAGE_DRIVERS_COUNT * CONCURRENCY <= msgCount);
	}
}
