package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.logging.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 12.06.17.
 */
public final class JsonTlsReadUsingInputFileTest
extends OldScenarioTestBase {

	private String stdOutput;
	private long actualTestTimeMilliseconds;

	private static final int EXPECTED_TEST_TIME_MINUTES = 3;

	public JsonTlsReadUsingInputFileTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return JsonTlsReadUsingInputFileTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "ReadUsingInputFile.json");
	}

	@Before
	public final void setUp()
	throws Exception {
		configArgs.add("--storage-net-ssl=true");
		super.setUp();
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		actualTestTimeMilliseconds = System.currentTimeMillis();
		scenario.run();
		actualTestTimeMilliseconds = System.currentTimeMillis() - actualTestTimeMilliseconds;
		LogUtil.flushAll();
		stdOutput = stdOutStream.stopRecordingAndGet();
	}

	@After
	public final void tearDown()
	throws Exception {
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {

		// test messages log file for the TLS enabled message
		final List<String> msgLogLines = getMessageLogLines();
		int msgCount = 0;
		for(final String msgLogLine : msgLogLines) {
			if(msgLogLine.contains(stepId + ": SSL/TLS is enabled for the channel")) {
				msgCount ++;
			}
		}
		// 2 steps + additional bucket checking/creating connections
		Assert.assertTrue(2 * driverCount.getValue() * concurrency.getValue() <= msgCount);

		// I/O traces
		final LongAdder ioTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceRecTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.READ.ordinal(), itemSize.getValue());
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceRecTestFunc);

		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.READ, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), 0, 0
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.CREATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		assertEquals(
			TimeUnit.MINUTES.toMillis(EXPECTED_TEST_TIME_MINUTES) + 10000,
			actualTestTimeMilliseconds,
			TimeUnit.MINUTES.toMillis(EXPECTED_TEST_TIME_MINUTES)
		);
	}
}
