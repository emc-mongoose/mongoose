package com.emc.mongoose.tests.system.deprecated;

import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.tests.system.base.HttpStorageDistributedScenarioTestBase;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.appenders.LoadJobLogFileManager;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.common.Constants.M;
import static com.emc.mongoose.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 28.03.17.
 */
public class ChainWithDelayTest
extends HttpStorageDistributedScenarioTestBase {
	
	private static final Path SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_SCENARIO, "mixed", "chain-with-delay.json"
	);
	private static final int DELAY_SECONDS = 60;
	private static final int TIME_LIMIT = 180;
	private static final String ZONE1_ADDR = "127.0.0.1";
	private static final String ZONE2_ADDR;
	static {
		try {
			ZONE2_ADDR = NetUtil.getHostAddrString();
		} catch(final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static boolean FINISHED_IN_TIME;
	
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		EnvUtil.set(
			new HashMap<String, String>() {
				{
					put("ZONE1_ADDRS", ZONE1_ADDR);
					put("ZONE2_ADDRS", ZONE2_ADDR);
				}
			}
		);
		JOB_NAME = ChainWithDelayTest.class.getSimpleName();
		ThreadContext.put(KEY_STEP_NAME, JOB_NAME);
		CONFIG_ARGS.add("--storage-driver-concurrency=10");
		CONFIG_ARGS.add("--test-scenario-file=" + SCENARIO_PATH.toString());
		CONFIG_ARGS.add("--test-step-limit-time=" + TIME_LIMIT);
		HttpStorageDistributedScenarioTestBase.setUpClass();
		final Thread runner = new Thread(
			() -> {
				try {
					SCENARIO.run();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				} finally {
					try {
						SCENARIO.close();
					} catch(final Throwable tt) {
						LogUtil.exception(Level.ERROR, tt, "Failed to close the scenario");
					}
				}
			}
		);
		runner.start();
		TimeUnit.SECONDS.timedJoin(runner, TIME_LIMIT + 5);
		FINISHED_IN_TIME = !runner.isAlive();
		runner.interrupt();
		LoadJobLogFileManager.flush(JOB_NAME);
		TimeUnit.SECONDS.sleep(10);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		HttpStorageDistributedScenarioTestBase.tearDownClass();
	}
	
	@Test
	public void testFinishedInTime() {
		assertTrue("Scenario didn't finished in time", FINISHED_IN_TIME);
	}
	
	@Test
	public void testIoTraceFile()
	throws Exception {
		final Map<String, Long> timingMap = new HashMap<>();
		String storageNode;
		String itemPath;
		IoType ioType;
		long reqTimeStart;
		long duration;
		Long prevOpFinishTime;
		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		for(final CSVRecord ioTraceRec : ioTraceRecords) {
			storageNode = ioTraceRec.get("StorageNode");
			itemPath = ioTraceRec.get("ItemPath");
			ioType = IoType.values()[Integer.parseInt(ioTraceRec.get("IoTypeCode"))];
			reqTimeStart = Long.parseLong(ioTraceRec.get("ReqTimeStart[us]"));
			duration = Long.parseLong(ioTraceRec.get("Duration[us]"));
			switch(ioType) {
				case CREATE:
					assertTrue(storageNode.startsWith(ZONE1_ADDR));
					timingMap.put(itemPath, reqTimeStart + duration);
					break;
				case READ:
					assertTrue(storageNode.startsWith(ZONE2_ADDR));
					prevOpFinishTime = timingMap.get(itemPath);
					if(prevOpFinishTime == null) {
						fail("No create I/O trace record for \"" + itemPath + "\"");
					} else {
						assertTrue((reqTimeStart - prevOpFinishTime) / M > DELAY_SECONDS);
					}
					break;
				default:
					fail("Unexpected I/O type: " + ioType);
			}
		}
	}
}
