package com.emc.mongoose.tests.system;

import com.github.akurilov.commons.net.NetUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.Constants.M;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 Created by kurila on 28.03.17.
 */
public class JsonChainWithDelayTest
extends OldScenarioTestBase {
	
	private static final int DELAY_SECONDS = 60;
	private static final int TIME_LIMIT = 180;

	private final String zone1Addr = "127.0.0.1";
	private String zone2Addr;
	private boolean finishedInTime;
	private String stdOutput;

	public JsonChainWithDelayTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "ChainWithDelay.json"
		);
	}

	@Override
	protected String makeStepId() {
		return JsonChainWithDelayTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}


	@Before
	public void setUp()
	throws Exception {
		try {
			zone2Addr = NetUtil.getHostAddrString();
		} catch(final Exception e) {
			throw new RuntimeException(e);
		}
		EnvUtil.set("ZONE1_ADDRS", zone1Addr);
		EnvUtil.set("ZONE2_ADDRS", zone2Addr);
		configArgs.add("--storage-net-http-namespace=ns1");
		configArgs.add("--test-step-limit-time=" + TIME_LIMIT);
		super.setUp();
		scenario = new JsonScenario(config, scenarioPath.toFile());
		final Thread runner = new Thread(
			() -> {
				try {
					scenario.run();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				} finally {
					try {
						scenario.close();
					} catch(final Throwable tt) {
						LogUtil.exception(Level.ERROR, tt, "Failed to close the scenario");
					}
				}
			}
		);
		stdOutStream.startRecording();
		runner.start();
		TimeUnit.SECONDS.timedJoin(runner, TIME_LIMIT + 10);
		finishedInTime = !runner.isAlive();
		runner.interrupt();
		LogUtil.flushAll();
		stdOutput = stdOutStream.stopRecordingAndGet();
		TimeUnit.SECONDS.sleep(10);
	}

	@After
	public final void tearDown()
	throws Exception {
		super.tearDown();
	}
	
	@Override
	public void test()
	throws Exception {

		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), 0,
			new HashMap<IoType, Integer>() {{
				put(IoType.CREATE, concurrency.getValue());
				put(IoType.READ, concurrency.getValue());
			}}
		);

		final Map<String, Long> timingMap = new HashMap<>();
		final Consumer<CSVRecord> ioTraceRecTestFunc = new Consumer<CSVRecord>() {

			String storageNode;
			String itemPath;
			IoType ioType;
			long reqTimeStart;
			long duration;
			Long prevOpFinishTime;

			@Override
			public final void accept(final CSVRecord ioTraceRec) {
				storageNode = ioTraceRec.get("StorageNode");
				itemPath = ioTraceRec.get("ItemPath");
				ioType = IoType.values()[Integer.parseInt(ioTraceRec.get("IoTypeCode"))];
				reqTimeStart = Long.parseLong(ioTraceRec.get("ReqTimeStart[us]"));
				duration = Long.parseLong(ioTraceRec.get("Duration[us]"));
				switch(ioType) {
					case CREATE:
						assertTrue(storageNode.startsWith(zone1Addr));
						timingMap.put(itemPath, reqTimeStart + duration);
						break;
					case READ:
						assertTrue(storageNode.startsWith(zone2Addr));
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
		};
		testIoTraceLogRecords(ioTraceRecTestFunc);

		assertTrue("Scenario didn't finished in time", finishedInTime);
	}
}
