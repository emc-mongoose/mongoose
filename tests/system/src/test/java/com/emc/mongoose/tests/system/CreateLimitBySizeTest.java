package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.deprecated.EnvConfiguredScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.Frequency;
import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 06.06.17.
 */

public class CreateLimitBySizeTest
extends ScenarioTestBase {
	
	private boolean finishedInTime = true;
	private String stdOutput = null;
	private final String itemOutputFile = CreateLimitBySizeTest.class.getSimpleName() + ".csv";
	private String itemOutputPath = null;
	private SizeInBytes sizeLimit;
	private long expectedCount;

	public CreateLimitBySizeTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return null;
	}

	@Override
	protected String makeStepId() {
		return CreateLimitBySizeTest.class.getSimpleName();
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		final SizeInBytes envItemSize = itemSize.getValue();
		final long envItemSizeValue = envItemSize.get();
		if(envItemSizeValue > SizeInBytes.toFixedSize("1GB")) {
			sizeLimit = new SizeInBytes(100 * envItemSizeValue);
		} else if(envItemSizeValue > SizeInBytes.toFixedSize("1MB")) {
			sizeLimit = new SizeInBytes(1_000 * envItemSizeValue);
		} else if(envItemSizeValue > SizeInBytes.toFixedSize("10KB")){
			sizeLimit = new SizeInBytes(10_000 * envItemSizeValue);
		} else {
			sizeLimit = new SizeInBytes(100_000 * envItemSizeValue);
		}
		expectedCount = sizeLimit.get() / envItemSizeValue;
		try {
			Files.delete(Paths.get(itemOutputFile));
		} catch(final Exception ignored) {
		}
		config.getItemConfig().getOutputConfig().setFile(itemOutputFile);
		config.getTestConfig().getStepConfig().getLimitConfig().setSize(sizeLimit);
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
		TimeUnit.SECONDS.timedJoin(runner, 1000);
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
			} catch(final IOException e) {
				e.printStackTrace(System.err);
			}
		}
		EnvConfiguredScenarioTestBase.tearDownClass();
	}

	@Override
	public void test()
	throws Exception {

		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertEquals(expectedCount, ioTraceRecords.size());
		final String nodeAddr = httpStorageMocks.keySet().iterator().next();
		for(final CSVRecord ioTraceRecord : ioTraceRecords) {
			testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), itemSize.getValue());
			HttpStorageMockUtil.assertItemExists(
				nodeAddr, ioTraceRecord.get("ItemPath"),
				Long.parseLong(ioTraceRecord.get("TransferSize"))
			);
		}

		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(itemOutputFile))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		assertEquals(expectedCount, items.size());
		final int itemIdRadix = config.getItemConfig().getNamingConfig().getRadix();
		final Frequency freq = new Frequency();
		String itemPath, itemId;
		long itemOffset;
		long size;
		String modLayerAndMask;
		for(final CSVRecord itemRec : items) {
			itemPath = itemRec.get(0);
			itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
			itemOffset = Long.parseLong(itemRec.get(1), 0x10);
			assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
			freq.addValue(itemOffset);
			size = Long.parseLong(itemRec.get(2));
			assertEquals(itemSize.getValue().get(), size);
			modLayerAndMask = itemRec.get(3);
			assertEquals("0/0", modLayerAndMask);
		}
		assertEquals(items.size(), freq.getUniqueCount());

		String msg = "Adjust output buffer size: " + itemSize.getValue().toString();
		int k;
		for(int i = 0; i < driverCount.getValue(); i ++) {
			k = stdOutput.indexOf(msg);
			if(k > -1) {
				msg = stdOutput.substring(k + msg.length());
			} else {
				fail(
					"Expected the message to occur " + driverCount.getValue() + " times, but got " +
						i
				);
			}
		}

		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0), IoType.CREATE, concurrency.getValue(),
			driverCount.getValue(), itemSize.getValue(), expectedCount, 0
		);

		testMetricsLogRecords(
			getMetricsLogRecords(), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), expectedCount, 0,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.CREATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), expectedCount,
			new HashMap<IoType, Integer>() {{ put(IoType.CREATE, concurrency.getValue()); }}
		);

		assertTrue(finishedInTime);
	}
}
