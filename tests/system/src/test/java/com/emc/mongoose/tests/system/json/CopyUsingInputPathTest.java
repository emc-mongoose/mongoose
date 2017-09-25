package com.emc.mongoose.tests.system.json;

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
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 12.06.17.
 */
public class CopyUsingInputPathTest
extends OldScenarioTestBase {

	private String itemSrcPath;
	private String itemDstPath;
	private String stdOutput;

	private static final int COUNT_LIMIT = 100_000;

	public CopyUsingInputPathTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "CopyUsingInputPath.json"
		);
	}

	@Override
	protected String makeStepId() {
		return CopyUsingInputPathTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		if(storageType.equals(StorageType.FS)) {
			itemSrcPath = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
			).toString();
		} else {
			itemSrcPath = '/' + stepId;
		}
		itemDstPath = itemSrcPath + "-Dst";
		itemSrcPath += "-Src";
		if(storageType.equals(StorageType.FS)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(itemSrcPath);
			} catch(final Throwable ignored) {
			}
			try {
				DirWithManyFilesDeleter.deleteExternal(itemDstPath);
			} catch(final Throwable ignored) {
			}
		}
		EnvUtil.set("ITEM_SRC_PATH", itemSrcPath);
		EnvUtil.set("ITEM_DST_PATH", itemDstPath);
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		scenario.run();
		LogUtil.flushAll();
		stdOutput = stdOutStream.stopRecordingAndGet();
	}

	@After
	public void tearDown()
	throws Exception {
		if(storageType.equals(StorageType.FS)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(itemSrcPath);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
			try {
				DirWithManyFilesDeleter.deleteExternal(itemDstPath);
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
		final LongAdder lostItemsCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceRecTestFunc;
		if(storageType.equals(StorageType.FS)) {
			ioTraceRecTestFunc = ioTraceRecord -> {
				File nextSrcFile;
				File nextDstFile;
				final String nextItemPath = ioTraceRecord.get("ItemPath");
				nextSrcFile = new File(nextItemPath);
				final String nextItemId = nextItemPath.substring(
					nextItemPath.lastIndexOf(File.separatorChar) + 1
				);
				nextDstFile = Paths.get(itemSrcPath, nextItemId).toFile();
				Assert.assertTrue(
					"File \"" + nextItemPath + "\" doesn't exist", nextSrcFile.exists()
				);
				if(!nextDstFile.exists()) {
					lostItemsCount.increment();
				} else {
					Assert.assertEquals(
						"Source file (" + nextItemPath + ") size (" + nextSrcFile.length() +
							" is not equal to the destination file (" + nextDstFile.getAbsolutePath() +
							") size (" + nextDstFile.length(),
						nextSrcFile.length(), nextDstFile.length()
					);
				}
				testIoTraceRecord(
					ioTraceRecord, IoType.CREATE.ordinal(), new SizeInBytes(nextSrcFile.length())
				);
				ioTraceRecCount.increment();
			};
		} else {
			final String node = httpStorageMocks.keySet().iterator().next();
			ioTraceRecTestFunc = ioTraceRecord -> {
				testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), itemSize.getValue());
				final String nextItemPath = ioTraceRecord.get("ItemPath");
				if(HttpStorageMockUtil.getContentLength(node, nextItemPath) < 0) {
					// not found
					lostItemsCount.increment();
				}
				final String nextItemId = nextItemPath.substring(
					nextItemPath.lastIndexOf(File.separatorChar) + 1
				);
				HttpStorageMockUtil.assertItemExists(
					node, itemSrcPath + '/' + nextItemId, itemSize.getValue().get()
				);
				ioTraceRecCount.increment();
			};
		}
		testIoTraceLogRecords(ioTraceRecTestFunc);

		assertTrue(
			"There should be " + COUNT_LIMIT + " records in the I/O trace log file",
			ioTraceRecCount.sum() <= COUNT_LIMIT
		);
		assertEquals(0, lostItemsCount.sum(), COUNT_LIMIT / 10_000);

		final List<CSVRecord> totalMetricsLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetricsLogRecords.size()
		);
		if(storageType.equals(StorageType.FS)) {
			// some files may remain not written fully
			testTotalMetricsLogRecord(
				totalMetricsLogRecords.get(0), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
				new SizeInBytes(itemSize.getValue().get() / 2, itemSize.getValue().get(), 1), 0, 0
			);
		} else {
			testTotalMetricsLogRecord(
				totalMetricsLogRecords.get(0), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
				itemSize.getValue(), 0, 0
			);
		}

		final List<CSVRecord> metricsLogRecords = getMetricsLogRecords();
		assertTrue(
			"There should be more than 0 metrics records in the log file",
			metricsLogRecords.size() > 0
		);
		if(storageType.equals(StorageType.FS)) {
			// some files may remain not written fully
			testMetricsLogRecords(
				metricsLogRecords, IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
				new SizeInBytes(itemSize.getValue().get() / 2, itemSize.getValue().get(), 1),
				0, 0, config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
			);
		} else {
			testMetricsLogRecords(
				metricsLogRecords, IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
				itemSize.getValue(), 0, 0,
				config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
			);
		}

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.CREATE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}
}
