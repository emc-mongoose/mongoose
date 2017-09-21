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
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.Frequency;
import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 Created by andrey on 06.02.17.
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 5. Circularity
 * 6.2.2. Limit Load Job by Processed Item Count
 * 8.2.1. Create New Items
 * 8.4.3.4. Append
 * 9.3. Custom Scenario File
 * 9.4.1. Override Default Configuration in the Scenario
 * 9.5.5. Sequential Job
 * 10.1.2. Two Local Separate Storage Driver Services (at different ports)
 */
public class CircularAppendTest
extends ScenarioTestBase {

	private static final int EXPECTED_APPEND_COUNT = 100;
	private static final long EXPECTED_COUNT = 100;
	private static final String ITEM_OUTPUT_FILE_1 = "CircularAppendTest1.csv";

	private String stdOutput;
	private String itemOutputPath;

	public CircularAppendTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "CircularAppend.json");
	}

	@Override
	protected String makeStepId() {
		return CircularAppendTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {
		configArgs.add("--storage-net-http-namespace=ns1");
		super.setUp();
		if(storageType.equals(StorageType.FS)) {
			itemOutputPath = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
			).toString();
			config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
		}
		EnvUtil.set("ITEM_DATA_SIZE", itemSize.getValue().toString());
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		scenario.run();
		stdOutput = stdOutStream.stopRecordingAndGet();
		LogUtil.flushAll();
		TimeUnit.SECONDS.sleep(10);
	}

	@After
	public void tearDown()
	throws Exception {
		if(storageType.equals(StorageType.FS)) {
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

		try {
			final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
			assertTrue(
				"There should be more than 0 metrics records in the log file",
				metricsLogRecords.size() > 0
			);
			testMetricsLogRecords(
				metricsLogRecords, IoType.UPDATE, concurrency.getValue(), driverCount.getValue(),
				itemSize.getValue(), (long) (1.1 * EXPECTED_APPEND_COUNT * EXPECTED_COUNT),
				0, config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
			);
		} catch(final FileNotFoundException ignored) {
			// there may be no metrics file if append step duration is less than 10s
		}

		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetrcisLogRecords.size()
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.UPDATE, concurrency.getValue(),
			driverCount.getValue(), itemSize.getValue(), 0, 0
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.UPDATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		final LongAdder ioTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.UPDATE.ordinal(), itemSize.getValue());
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceReqTestFunc);
		assertTrue(
			"There should be more than " + EXPECTED_COUNT +
				" records in the I/O trace log file, but got: " + ioTraceRecCount.sum(),
			EXPECTED_COUNT < ioTraceRecCount.sum()
		);


		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(ITEM_OUTPUT_FILE_1))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		final int itemIdRadix = config.getItemConfig().getNamingConfig().getRadix();
		final Frequency freq = new Frequency();
		String itemPath, itemId;
		long itemOffset;
		long size;
		final SizeInBytes expectedFinalSize = new SizeInBytes(
			(EXPECTED_APPEND_COUNT + 1) * itemSize.getValue().get() / 3,
			3 * (EXPECTED_APPEND_COUNT + 1) * itemSize.getValue().get(),
			1
		);
		final int n = items.size();
		CSVRecord itemRec;
		for(int i = 0; i < n; i ++) {
			itemRec = items.get(i);
			itemPath = itemRec.get(0);
			for(int j = i; j < n; j ++) {
				if(i != j) {
					assertFalse(itemPath.equals(items.get(j).get(0)));
				}
			}
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			if(!storageType.equals(StorageType.ATMOS)) {
				itemOffset = Long.parseLong(itemRec.get(1), 0x10);
				assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
				freq.addValue(itemOffset);
			}
			size = Long.parseLong(itemRec.get(2));
			assertTrue(
				"Expected size: " + expectedFinalSize.toString() + ", actual: " + size,
				expectedFinalSize.getMin() <= size && size <= expectedFinalSize.getMax()
			);
			assertEquals("0/0", itemRec.get(3));
		}
		if(!storageType.equals(StorageType.ATMOS)) {
			assertEquals(EXPECTED_COUNT, freq.getUniqueCount(), EXPECTED_COUNT / 20);
		}
	}
}
