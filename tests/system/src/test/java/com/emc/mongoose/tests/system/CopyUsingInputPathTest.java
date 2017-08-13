package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

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

/**
 Created by andrey on 12.06.17.
 */
public class CopyUsingInputPathTest
extends ScenarioTestBase {

	private String itemSrcPath;
	private String itemDstPath;
	private String stdOutput;

	private static final int COUNT_LIMIT = 500_000;

	public CopyUsingInputPathTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "CopyUsingInputPath.json"
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
		itemDstPath = itemSrcPath + "Dst";
		itemSrcPath += "Src";
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

		final List<CSVRecord> ioTraceRecords = getIoTraceLogRecords();
		assertTrue(
			"There should be " + COUNT_LIMIT + " records in the I/O trace log file",
			ioTraceRecords.size() <= COUNT_LIMIT
		);
		String nextItemPath, nextItemId;
		if(storageType.equals(StorageType.FS)) {
			File nextSrcFile;
			File nextDstFile;
			for(final CSVRecord ioTraceRecord : ioTraceRecords) {
				nextItemPath = ioTraceRecord.get("ItemPath");
				nextSrcFile = new File(nextItemPath);
				nextItemId = nextItemPath.substring(
					nextItemPath.lastIndexOf(File.separatorChar) + 1
				);
				nextDstFile = Paths.get(itemSrcPath, nextItemId).toFile();
				Assert.assertTrue(
					"File \"" + nextItemPath + "\" doesn't exist", nextSrcFile.exists()
				);
				Assert.assertTrue(
					"File \"" + nextDstFile.getPath() + "\" doesn't exist", nextDstFile.exists()
				);
				Assert.assertEquals(nextSrcFile.length(), nextDstFile.length());
				testIoTraceRecord(
					ioTraceRecord, IoType.CREATE.ordinal(), new SizeInBytes(nextSrcFile.length())
				);
			}
		} else {
			final String node = httpStorageMocks.keySet().iterator().next();
			for(final CSVRecord ioTraceRecord : ioTraceRecords) {
				testIoTraceRecord(ioTraceRecord, IoType.CREATE.ordinal(), itemSize.getValue());
				nextItemPath = ioTraceRecord.get("ItemPath");
				HttpStorageMockUtil.assertItemExists(node, nextItemPath, 0);
				nextItemId = nextItemPath.substring(
					nextItemPath.lastIndexOf(File.separatorChar) + 1
				);
				HttpStorageMockUtil.assertItemExists(
					node, itemSrcPath + '/' + nextItemId, itemSize.getValue().get()
				);
			}
		}

		final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords();
		assertEquals(
			"There should be 1 total metrics records in the log file", 1,
			totalMetrcisLogRecords.size()
		);
		if(storageType.equals(StorageType.FS)) {
			// some files may remain not written fully
			testTotalMetricsLogRecord(
				totalMetrcisLogRecords.get(0), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
				new SizeInBytes(itemSize.getValue().get() / 2, itemSize.getValue().get(), 1), 0, 0
			);
		} else {
			testTotalMetricsLogRecord(
				totalMetrcisLogRecords.get(0), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
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
