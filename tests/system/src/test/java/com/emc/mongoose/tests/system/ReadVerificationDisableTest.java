package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 12.06.17.
 */
public final class ReadVerificationDisableTest
extends ScenarioTestBase {

	private static final int EXPECTED_MAX_COUNT = 1_000_000;
	private static final SizeInBytes EXPECTED_MAX_SIZE = new SizeInBytes("1GB");

	private String itemOutputPath;
	private String stdOutput;

	public ReadVerificationDisableTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return ReadVerificationDisableTest.class.getSimpleName() + '-' + storageType.name() +
			'-' + driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "ReadVerificationDisable.json");
	}

	@Before
	public final void setUp()
	throws Exception {
		configArgs.add("--storage-net-http-namespace=ns1");
		super.setUp();
		if(StorageType.FS.equals(storageType)) {
			itemOutputPath = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
			).toString();
			config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
		}
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		scenario.run();
		LogUtil.flushAll();
		stdOutput = stdOutStream.stopRecordingAndGet();
		TimeUnit.SECONDS.sleep(5);
	}

	@After
	public final void tearDown()
	throws Exception {
		if(StorageType.FS.equals(storageType)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(itemOutputPath);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {

		// I/O traces
		final LongAdder ioTraceRecCount = new LongAdder();
		final LongAdder transferSize = new LongAdder();
		final Consumer<CSVRecord> ioTraceRecTestFunc = ioTraceRec -> {
			assertEquals(
				"Record #" + ioTraceRecCount.sum() + ": unexpected operation type " + ioTraceRec.get("IoTypeCode"),
				IoType.READ, IoType.values()[Integer.parseInt(ioTraceRec.get("IoTypeCode"))]
			);
			assertEquals(
				"Record #" + ioTraceRecCount.sum() + ": unexpected status code " + ioTraceRec.get("StatusCode"),
				IoTask.Status.SUCC,
				IoTask.Status.values()[Integer.parseInt(ioTraceRec.get("StatusCode"))]
			);
			transferSize.add(Long.parseLong(ioTraceRec.get("TransferSize")));
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceRecTestFunc);
		assertTrue(EXPECTED_MAX_COUNT >= ioTraceRecCount.sum());
		assumeTrue(EXPECTED_MAX_SIZE.get() >= transferSize.sum());

		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), 0, 0
		);

		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), 0, 0,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), 0,
			new HashMap<IoType, Integer>() {{
				put(IoType.CREATE, concurrency.getValue());
				put(IoType.UPDATE, concurrency.getValue());
				put(IoType.READ, concurrency.getValue());
			}}
		);
	}
}
