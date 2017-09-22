package com.emc.mongoose.tests.system.json;

import com.github.akurilov.commons.system.SizeInBytes;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
 Created by andrey on 13.06.17.
 */
public class MultiPartCreateTest
extends ScenarioTestBase {

	private String stdOutput;
	private long expectedCountMin;
	private long expectedCountMax;
	private SizeInBytes partSize;
	private SizeInBytes fullItemSize;
	private SizeInBytes sizeLimit;
	
	private String itemOutputFile = MultiPartCreateTest.class.getSimpleName() + "Items.csv";

	public MultiPartCreateTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "MultiPartCreate.json");
	}

	@Override
	protected String makeStepId() {
		return MultiPartCreateTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		partSize = itemSize.getValue();
		fullItemSize = new SizeInBytes(partSize.get(), 100 * partSize.get(), 3);
		config.getItemConfig().getDataConfig().setSize(fullItemSize);
		Loggers.MSG.info("Item size: {}, part size: {}", fullItemSize, partSize);
		EnvUtil.set("PART_SIZE", partSize.toString());
		EnvUtil.set("ITEM_OUTPUT_FILE", itemOutputFile);
		sizeLimit = new SizeInBytes(
			(driverCount.getValue() + 1) * concurrency.getValue() * fullItemSize.getAvg()
		);
		Loggers.MSG.info("Use the size limit: {}", sizeLimit);
		EnvUtil.set("SIZE_LIMIT", sizeLimit.toString());
		expectedCountMin = sizeLimit.get() / fullItemSize.getMax();
		expectedCountMax = sizeLimit.get() / fullItemSize.getMin();
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		scenario.run();
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

		final LongAdder ioTraceRecCount = new LongAdder();
		final SizeInBytes ZERO_SIZE = new SizeInBytes(0);
		final SizeInBytes TAIL_PART_SIZE = new SizeInBytes(1, partSize.get(), 1);
		final Consumer<CSVRecord> ioTraceRecFunc = ioTraceRec -> {
			try {
				testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), ZERO_SIZE);
			} catch(final AssertionError e) {
				try {
					testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), partSize);
				} catch(final AssertionError ee) {
					testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), TAIL_PART_SIZE);
				}
			}
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceRecFunc);

		final List<CSVRecord> itemRecs = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(itemOutputFile))) {
			try(final CSVParser csvParser = CSVFormat.RFC4180.parse(br)) {
				for(final CSVRecord csvRecord : csvParser) {
					itemRecs.add(csvRecord);
				}
			}
		}
		long nextItemSize;
		long sizeSum = 0;
		final int n = itemRecs.size();
		assertTrue(n > 0);
		assertTrue(
			"Expected no less than " + expectedCountMin + " items, but got " + n,
			expectedCountMin <= n
		);
		assertTrue(
			"Expected no more than " + expectedCountMax + " items, but got " + n,
			expectedCountMax >= n
		);
		for(final CSVRecord itemRec : itemRecs) {
			nextItemSize = Long.parseLong(itemRec.get(2));
			assertTrue(fullItemSize.getMin() <= nextItemSize);
			assertTrue(fullItemSize.getMax() >= nextItemSize);
			sizeSum += nextItemSize;
		}
		final long delta = driverCount.getValue() * concurrency.getValue() * partSize.getMax();
		System.out.println(
			"Expected transfer size: " + sizeLimit.get() + "+" + delta +", actual: " + sizeSum
		);
		assertTrue(
			"Expected to transfer no more than " + sizeLimit + "+" + delta
				+ ", but transferred actually: " + new SizeInBytes(sizeSum),
			sizeLimit.get() + delta >= sizeSum
		);

		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetrcisLogRecords.size()
		);
		testTotalMetricsLogRecord(
			totalMetrcisLogRecords.get(0), IoType.CREATE, concurrency.getValue(),
			driverCount.getValue(), fullItemSize, 0, 0
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.CREATE, concurrency.getValue(), driverCount.getValue(), fullItemSize,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}
}
