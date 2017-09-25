package com.emc.mongoose.tests.system.groovy;

import com.emc.mongoose.tests.system.base.Jsr223ScenarioTestBase;
import com.emc.mongoose.tests.system.util.docker.WaitResponseCallback;
import com.github.akurilov.commons.system.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.WaitResponse;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.Frequency;
import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;

import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.Closeable;
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

public class CreateLimitBySizeTest
extends Jsr223ScenarioTestBase {
	
	private boolean finishedInTime = true;
	private String stdOutput = null;
	private final String itemOutputFile = CreateLimitBySizeTest.class.getSimpleName() + ".csv";
	private String itemOutputPath = null;
	private SizeInBytes sizeLimit;
	private long expectedCount;
	private int containerExitCode;

	public CreateLimitBySizeTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "groovy", "default.groovy");
	}

	@Override
	protected String makeStepId() {
		return CreateLimitBySizeTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {

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
		configArgs.add("--item-output-file=" + itemOutputFile);
		configArgs.add("--test-step-limit-size=" + sizeLimit);
		switch(storageType) {
			case FS:
				itemOutputPath = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
				).toString();
				configArgs.add("--item-output-path=" + itemOutputPath);
				break;
			case SWIFT:
				configArgs.add("--storage-net-http-namespace=ns1");
				break;
		}

		super.setUp();

		dockerClient.startContainerCmd(testContainerId).exec();
		containerExitCode = dockerClient
			.waitContainerCmd(testContainerId)
			.exec(new WaitContainerResultCallback())
			.awaitStatusCode(1000, TimeUnit.SECONDS);
		stdOutput = stdOutBuff.toString();
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
		testIoTraceLogRecords(ioTraceRecFunc);
		assertEquals(expectedCount, ioTraceRecCount.sum());

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
					"Expected the message to occur " + driverCount.getValue() + " times, but got "
						+ i
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
