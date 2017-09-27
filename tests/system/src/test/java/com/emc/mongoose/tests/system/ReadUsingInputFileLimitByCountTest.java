package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 07.06.17.
 */
public final class ReadUsingInputFileLimitByCountTest
extends OldScenarioTestBase {

	private static final long EXPECTED_COUNT = 10_000;

	private String stdOutput = null;
	private String itemOutputPath = null;

	public ReadUsingInputFileLimitByCountTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return ReadUsingInputFileLimitByCountTest.class.getSimpleName() + '-' + storageType.name() +
			'-' + driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "ReadUsingInputFileLimitByCount.json"
		);
	}

	@Before
	public final void setUp()
	throws Exception {
		super.setUp();
		switch(storageType) {
			case FS:
				itemOutputPath = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
				).toString();
				config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
				break;
			case SWIFT:
				config.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
				break;
		}
		try {
			scenario = new JsonScenario(config, scenarioPath.toFile());
			stdOutStream.startRecording();
			scenario.run();
			stdOutput = stdOutStream.stopRecordingAndGet();
		} catch(final Throwable t) {
			LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
		}
		LogUtil.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}

	@After
	public final void tearDown()
	throws Exception {
		if(StorageType.FS.equals(storageType)) {
			try {
				FileUtils.deleteDirectory(new File(itemOutputPath));
			} catch(final IOException e) {
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
		final Consumer<CSVRecord> ioTraceRecTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.READ.ordinal(), itemSize.getValue());
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceRecTestFunc);
		assertEquals(EXPECTED_COUNT, ioTraceRecCount.sum());

		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), EXPECTED_COUNT, 0
		);

		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			EXPECTED_COUNT, 0, config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}
}
