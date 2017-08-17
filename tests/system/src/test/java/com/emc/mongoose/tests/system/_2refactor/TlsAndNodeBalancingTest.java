package com.emc.mongoose.tests.system._2refactor;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.deprecated.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.ui.log.LogUtil;
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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 Created by andrey on 12.06.17.
 */
public final class TlsAndNodeBalancingTest
extends ScenarioTestBase {

	private static String STD_OUTPUT;
	private static long ACTUAL_TEST_TIME_MILLISECONDS;

	private static final int EXPECTED_TEST_TIME_MINUTES = 3;

	@Before
	public final void setUp()
	throws Exception {
		EXCLUDE_PARAMS.clear();
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_TYPE, Arrays.asList("fs"));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_COUNT, Arrays.asList(2));
		EXCLUDE_PARAMS.put(KEY_ENV_STORAGE_DRIVER_concurrency.getValue(), Arrays.asList(100, 1000));
		EXCLUDE_PARAMS.put(KEY_ENV_itemSize.getValue(), Arrays.asList(new SizeInBytes("10GB")));
		stepId = TlsAndNodeBalancingTest.class.getSimpleName();
		scenarioPath = Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "ReadUsingInputPath.json");
		ThreadContext.put(KEY_TEST_STEP_ID, stepId);
		HTTP_STORAGE_NODE_COUNT = 4;
		configArgs.add("--storage-mock-node=true");
		configArgs.add("--storage-net-ssl=true");
		super.setUp();
		if(SKIP_FLAG) {
			return;
		}
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		ACTUAL_TEST_TIME_MILLISECONDS = System.currentTimeMillis();
		scenario.run();
		ACTUAL_TEST_TIME_MILLISECONDS = System.currentTimeMillis() - ACTUAL_TEST_TIME_MILLISECONDS;
		LogUtil.flushAll();
		STD_OUTPUT = stdOutStream.stopRecordingAndGet();
	}

	@After
	public final void tearDown()
	throws Exception {
		super.tearDown();
	}

	@Test
	public void testFinishedInTime() {
		assumeFalse(SKIP_FLAG);
		assertEquals(
			TimeUnit.MINUTES.toMillis(EXPECTED_TEST_TIME_MINUTES) + 10000,
			ACTUAL_TEST_TIME_MILLISECONDS,
			TimeUnit.MINUTES.toMillis(EXPECTED_TEST_TIME_MINUTES)
		);
	}

	@Test
	public void testTotalMetricsLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), 0, 0
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(1), IoType.READ, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), 0, 0
		);
	}

	@Test
	public void testMetricsStdout()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.CREATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testSingleMetricsStdout(
			STD_OUTPUT.replaceAll("[\r\n]+", " "),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}

	@Test
	public void testIoTraceLogFile()
	throws Exception {
		assumeFalse(SKIP_FLAG);
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		String storageNodeAddr;
		final Object2IntMap<String> nodeFreq = new Object2IntOpenHashMap<>();
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.READ.ordinal(), itemSize.getValue());
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
		assumeFalse(SKIP_FLAG);
		final List<String> msgLogLines = getMessageLogLines();
		int msgCount = 0;
		for(final String msgLogLine : msgLogLines) {
			if(msgLogLine.contains(stepId + ": SSL/TLS is enabled for the channel")) {
				msgCount ++;
			}
		}
		// 3 steps + additional bucket checking/creating connections
		Assert.assertTrue(3 * driverCount.getValue() * concurrency.getValue() <= msgCount);
	}
}
