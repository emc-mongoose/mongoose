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
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 07.06.17.
 */
public class UpdateUsingInputFileLimitByTimeTest
extends ScenarioTestBase {

	private final int expectedTime = 25;
	private String stdOutput = null;
	private String itemOutputPath = null;

	public UpdateUsingInputFileLimitByTimeTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "UpdateUsingInputFileLimitByTime.json"
		);
	}

	@Override
	protected String makeStepId() {
		return UpdateUsingInputFileLimitByTimeTest.class.getSimpleName() + '-' + storageType.name()
			+ '-' + driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		switch(storageType) {
			case FS:
				itemOutputPath = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
				).toString();
				config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
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
	public void tearDown()
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

		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader("UpdateUsingInputFileLimitByTimeTest_.csv"))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		String itemPath;
		String itemId;
		long itemOffset;
		final int itemIdRadix = config.getItemConfig().getNamingConfig().getRadix();
		long actualItemSize;
		String[] modLayerAndMask;
		int layer;
		String rangesMask;
		char[] rangesMaskChars;
		BitSet mask;
		for(final CSVRecord itemRec : items) {
			itemPath = itemRec.get(0);
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			itemOffset = Long.parseLong(itemRec.get(1), 0x10);
			assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
			actualItemSize = Long.parseLong(itemRec.get(2));
			assertEquals(itemSize.getValue().get(), actualItemSize);
			modLayerAndMask = itemRec.get(3).split("/");
			assertEquals("Modification record should contain 2 parts", 2, modLayerAndMask.length);
			layer = Integer.parseInt(modLayerAndMask[0], 0x10);
			assertEquals(layer, 0);
			rangesMask = modLayerAndMask[1];
			if(rangesMask.length() == 0) {
				rangesMaskChars = ("00" + rangesMask).toCharArray();
			} else if(rangesMask.length() % 2 == 1) {
				rangesMaskChars = ("0" + rangesMask).toCharArray();
			} else {
				rangesMaskChars = rangesMask.toCharArray();
			}
			mask = BitSet.valueOf(Hex.decodeHex(rangesMaskChars));
			assertTrue(
				"The modification record \"" + itemRec.get(3) + "\" is not updated",
				mask.cardinality() > 0
			);
		}

		final SizeInBytes updateSize = new SizeInBytes(1, itemSize.getValue().get() / 2 + 1, 1);
		final LongAdder ioTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.UPDATE.ordinal(), updateSize);
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceReqTestFunc);
		assertTrue(ioTraceRecCount.sum() > 0);

		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.UPDATE, concurrency.getValue(), driverCount.getValue(), updateSize, 0, expectedTime
		);

		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		final long metricsPeriod = config
			.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod();
		assertTrue(metricsLogRecords.size() <= expectedTime / metricsPeriod + 1);
		testMetricsLogRecords(
			metricsLogRecords, IoType.UPDATE, concurrency.getValue(), driverCount.getValue(), updateSize, 0, 0,
			metricsPeriod
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.UPDATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), 0,
			new HashMap<IoType, Integer>() {{ put(IoType.UPDATE, concurrency.getValue() ); }}
		);
	}
}
