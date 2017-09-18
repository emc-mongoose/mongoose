package com.emc.mongoose.tests.system;

import com.github.akurilov.commons.system.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.LongStream;

/**
 Created by kurila on 15.06.17.
 */
public final class MultipleFixedUpdateAndSingleFixedReadTest
extends ScenarioTestBase {
	
	private String itemOutputPath;
	private String stdOutput;
	private SizeInBytes expectedUpdateSize;
	private SizeInBytes expectedReadSize;
	
	private static final long EXPECTED_COUNT = 2000;

	public MultipleFixedUpdateAndSingleFixedReadTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return MultipleFixedUpdateAndSingleFixedReadTest.class.getSimpleName() + '-' +
			storageType.name() + '-' + driverCount.name() + 'x' + concurrency.name() + '-' +
			itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "MultipleFixedUpdateAndSingleFixedRead.json"
		);
	}

	@Before
	public final void setUp()
	throws Exception {
		// https://github.com/emc-mongoose/nagaina/issues/3
		super.setUp();
		expectedUpdateSize = new SizeInBytes(
			-LongStream.of(2-5,10-20,50-100,200-500,1000-2000).sum()
		);
		expectedReadSize = new SizeInBytes(itemSize.getValue().get() - 256);
		if(StorageType.FS.equals(storageType)) {
			itemOutputPath = Paths
				.get(Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId)
				.toString();
			config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
		}
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		scenario.run();
		LogUtil.flushAll();
		stdOutput = stdOutStream.stopRecordingAndGet();
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
		final Consumer<CSVRecord> ioTraceRecTestFunc = ioTraceRec -> {
			if(ioTraceRecCount.sum() < EXPECTED_COUNT) {
				testIoTraceRecord(ioTraceRec, IoType.UPDATE.ordinal(), expectedUpdateSize);
			} else {
				testIoTraceRecord(ioTraceRec, IoType.READ.ordinal(), expectedReadSize);
			}
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceRecTestFunc);
		assertEquals(
			"There should be " + 2 * EXPECTED_COUNT + " records in the I/O trace log file",
			2 * EXPECTED_COUNT, ioTraceRecCount.sum()
		);

		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.UPDATE, concurrency.getValue(),
			driverCount.getValue(), expectedUpdateSize, EXPECTED_COUNT, 0
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(1), IoType.READ, concurrency.getValue(),
			driverCount.getValue(), expectedReadSize, EXPECTED_COUNT, 0
		);

		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		final List<CSVRecord> updateMetricsRecords = new ArrayList<>();
		final List<CSVRecord> readMetricsRecords = new ArrayList<>();
		for(final CSVRecord metricsLogRec : metricsLogRecords) {
			if(IoType.UPDATE.name().equalsIgnoreCase(metricsLogRec.get("TypeLoad"))) {
				updateMetricsRecords.add(metricsLogRec);
			} else {
				readMetricsRecords.add(metricsLogRec);
			}
		}
		testMetricsLogRecords(
			updateMetricsRecords, IoType.UPDATE, concurrency.getValue(), driverCount.getValue(),
			expectedUpdateSize, EXPECTED_COUNT, 0,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsLogRecords(
			readMetricsRecords, IoType.READ, concurrency.getValue(), driverCount.getValue(),
			expectedReadSize, EXPECTED_COUNT, 0,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		final String stdOutput = this.stdOutput.replaceAll("[\r\n]+", " ");
		testSingleMetricsStdout(
			stdOutput, IoType.UPDATE, concurrency.getValue(), driverCount.getValue(),
			expectedUpdateSize,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testSingleMetricsStdout(
			stdOutput, IoType.READ, concurrency.getValue(), driverCount.getValue(),
			expectedReadSize,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}
}
