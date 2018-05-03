package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.logging.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 06.02.17.
 Covered use cases:
 * 2.1.1.1.3. Intermediate Size Data Items (100KB-10MB)
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 4.2. Small Concurrency Level (2-10)
 * 5. Circularity
 * 6.2.2. Limit Load Job by Processed Item Count
 * 6.2.5. Limit Load Job by Time
 * 8.2.1. Create New Items
 * 8.3.1. Read With Disabled Validation
 * 9.3. Custom Scenario File
 * 9.4.1. Override Default Configuration in the Scenario
 * 9.5.5. Sequential Job
 * 10.1.2. Two Local Separate Storage Driver Services (at different ports)
 */

public class JsonCircularReadSingleItemTest
extends OldScenarioTestBase {

	private static final String ITEM_OUTPUT_FILE = "CircularReadSingleItem.csv";
	private String stdOutput;
	private boolean finishedInTime;
	private String itemOutputPath;

	public JsonCircularReadSingleItemTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return JsonCircularReadSingleItemTest.class.getSimpleName() + '-' + storageType.name()
			+ '-' + driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "CircularReadSingleItem.json"
		);
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
		scenario = new JsonScenario(config, scenarioPath.toFile());
		final Thread runner = new Thread(
			() -> {
				try {
					stdOutStream.startRecording();
					scenario.run();
					stdOutput = stdOutStream.stopRecordingAndGet();
				} catch(final Throwable t) {
					LogUtil.exception(Level.ERROR, t, "Failed to run the scenario");
				}
			}
		);
		runner.start();
		TimeUnit.MINUTES.timedJoin(runner, 65); // 1m + up to 5s for the precondition job
		finishedInTime = !runner.isAlive();
		runner.interrupt();
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

		final LongAdder ioTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.READ.ordinal(), itemSize.getValue());
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceReqTestFunc);
		assertTrue(
			"There should be more than 1 record in the I/O trace log file",
			ioTraceRecCount.sum() > 1
		);

		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(ITEM_OUTPUT_FILE))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		assertEquals(1, items.size());
		final int itemIdRadix = config.getItemConfig().getNamingConfig().getRadix();
		String itemPath, itemId;
		long itemOffset;
		long size;
		String modLayerAndMask;
		final CSVRecord itemRec = items.get(0);
		itemPath = itemRec.get(0);
		itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
		itemOffset = Long.parseLong(itemRec.get(1), 0x10);
		assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
		size = Long.parseLong(itemRec.get(2));
		assertEquals(itemSize.getValue().get(), size);
		modLayerAndMask = itemRec.get(3);
		assertEquals("0/0", modLayerAndMask);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.CREATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testFinalMetricsTableRowStdout(
			stdOutput, stepId, IoType.CREATE, driverCount.getValue(), concurrency.getValue(),
			0, 60, itemSize.getValue()
		);

		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetrcisLogRecords.size()
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.READ, concurrency.getValue(),
			driverCount.getValue(), itemSize.getValue(), 0, 60
		);

		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		assertTrue(
			"There should be more than 2 metrics records in the log file",
			metricsLogRecords.size() > 1
		);
		testMetricsLogRecords(
			metricsLogRecords, IoType.READ, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), 0, 60,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		assertTrue("Scenario didn't finished in time", finishedInTime);
	}
}
