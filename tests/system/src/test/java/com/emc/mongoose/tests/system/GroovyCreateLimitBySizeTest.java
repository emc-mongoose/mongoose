package com.emc.mongoose.tests.system;

import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.github.akurilov.commons.system.SizeInBytes;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.Frequency;
import org.junit.After;
import org.junit.Before;

import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by kurila on 06.06.17.
 */

public class GroovyCreateLimitBySizeTest
extends ScenarioTestBase {

	private String stdOutput = null;
	private final String containerItemOutputFile = CONTAINER_SHARE_PATH + "/"
		+ GroovyCreateLimitBySizeTest.class.getSimpleName() + ".csv";
	private final String hostItemOutputFile = HOST_SHARE_PATH + File.separator
		+ GroovyCreateLimitBySizeTest.class.getSimpleName() + ".csv";
	private String containerItemOutputPath = null;
	private String hostItemOutputPath = null;
	private SizeInBytes sizeLimit;
	private long expectedCount;
	private long duration;
	private int containerExitCode;

	public GroovyCreateLimitBySizeTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(DIR_EXAMPLE_SCENARIO, "groovy", "default.groovy");
	}

	@Override
	protected String makeStepId() {
		return GroovyCreateLimitBySizeTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
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
			Files.delete(Paths.get(hostItemOutputFile));
		} catch(final Exception ignored) {
		}
		configArgs.add("--item-output-file=" + containerItemOutputFile);
		configArgs.add("--test-step-limit-size=" + sizeLimit);
		switch(storageType) {
			case FS:
				containerItemOutputPath = Paths.get(CONTAINER_SHARE_PATH, stepId).toString();
				hostItemOutputPath = HOST_SHARE_PATH.toString() + File.separator + stepId;
				configArgs.add("--item-output-path=" + containerItemOutputPath);
				break;
			case SWIFT:
				configArgs.add("--storage-net-http-namespace=ns1");
				break;
		}

		initTestContainer();

		duration = System.currentTimeMillis();
		dockerClient.startContainerCmd(testContainerId).exec();
		containerExitCode = dockerClient
			.waitContainerCmd(testContainerId)
			.exec(new WaitContainerResultCallback())
			.awaitStatusCode(1000, TimeUnit.SECONDS);
		duration = System.currentTimeMillis() - duration;
		stdOutput = stdOutBuff.toString();
	}
	
	@After
	public void tearDown()
	throws Exception {
		if(storageType.equals(StorageType.FS)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(containerItemOutputPath);
			} catch(final IOException e) {
				e.printStackTrace(System.err);
			}
		}
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {

		assertEquals("Container exit code should be 0", 0, containerExitCode);

		final LongAdder ioTraceRecCount = new LongAdder();
		final String nodeAddr = httpStorageMocks.keySet().iterator().next();
		final Consumer<CSVRecord> ioTraceRecFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), itemSize.getValue());
			HttpStorageMockUtil.assertItemExists(
				nodeAddr, ioTraceRec.get("ItemPath"),
				Long.parseLong(ioTraceRec.get("TransferSize"))
			);
			ioTraceRecCount.increment();
		};
		testContainerIoTraceLogRecords(ioTraceRecFunc);
		assertEquals(expectedCount, ioTraceRecCount.sum(), expectedCount / 1000);

		final List<CSVRecord> items = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(hostItemOutputFile))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			for(final CSVRecord csvRecord : csvParser) {
				items.add(csvRecord);
			}
		}
		assertEquals(expectedCount, items.size(), expectedCount / 1000);
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
					"Expected the message to occur " + driverCount.getValue() + " times, but got "
						+ i
				);
			}
		}

		testTotalMetricsLogRecord(
			getContainerMetricsTotalLogRecords().get(0), IoType.CREATE, concurrency.getValue(),
			driverCount.getValue(), itemSize.getValue(), 0, 0
		);

		testMetricsLogRecords(
			getContainerMetricsLogRecords(), IoType.CREATE, concurrency.getValue(),
			driverCount.getValue(), itemSize.getValue(), 0, 0,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.CREATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), 0,
			new HashMap<IoType, Integer>() {{ put(IoType.CREATE, concurrency.getValue()); }}
		);

		assertTrue(duration < 1000000);
	}
}
