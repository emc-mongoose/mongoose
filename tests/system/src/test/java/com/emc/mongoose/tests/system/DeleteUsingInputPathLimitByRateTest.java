package com.emc.mongoose.tests.system;

import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.github.akurilov.commons.system.SizeInBytes;
import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.tests.system.util.HttpStorageMockUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 07.06.17.
 */
public class DeleteUsingInputPathLimitByRateTest
extends ScenarioTestBase {

	private static final int EXPECTED_COUNT = 10_000;
	private static final double EXPECTED_RATE = 234.5;

	private String stdOutput = null;
	private String itemOutputPath = null;

	public DeleteUsingInputPathLimitByRateTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return DeleteUsingInputPathLimitByRateTest.class.getSimpleName() + '-' +
			storageType.name() + '-' + driverCount.name() + 'x' + concurrency.name() + '-' +
			itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "DeleteUsingInputPathLimitByRate.json"
		);
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
				try {
					DirWithManyFilesDeleter.deleteExternal(itemOutputPath);
				} catch(final Throwable ignored) {
				}
				EnvUtil.set("ITEMS_PATH", itemOutputPath);
				break;
			case SWIFT:
				config.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
			default:
				EnvUtil.set("ITEMS_PATH", "/" + stepId);
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

		// I/O traces
		final LongAdder ioTraceRecCount = new LongAdder();
		final String nodeAddr = config.getStorageConfig().getNetConfig().getNodeConfig().getAddrs().get(0);
		final Consumer<CSVRecord> ioTraceRecTestFunc;
		if(StorageType.FS.equals(storageType)) {
			ioTraceRecTestFunc = ioTraceRec -> {
				testIoTraceRecord(ioTraceRec, IoType.DELETE.ordinal(), new SizeInBytes(0));
				final String nextItemPath = ioTraceRec.get("ItemPath");
				assertFalse(Files.exists(Paths.get(nextItemPath)));
				ioTraceRecCount.increment();
			};
		} else {
			ioTraceRecTestFunc = ioTraceRec -> {
				testIoTraceRecord(ioTraceRec, IoType.DELETE.ordinal(), new SizeInBytes(0));
				final String nextItemPath = ioTraceRec.get("ItemPath");
				HttpStorageMockUtil.assertItemNotExists(nodeAddr, nextItemPath);
				ioTraceRecCount.increment();
			};
		}
		testIoTraceLogRecords(ioTraceRecTestFunc);
		assertTrue("Expected at least one I/O trace record", ioTraceRecCount.sum() > 0);

		// total metrics log file
		final CSVRecord totalRec = getMetricsTotalLogRecords().get(0);
		testTotalMetricsLogRecord(
			totalRec, IoType.DELETE, concurrency.getValue(), driverCount.getValue(),
			new SizeInBytes(0), EXPECTED_COUNT, 0
		);
		assertEquals(
			EXPECTED_RATE, Double.parseDouble(totalRec.get("TPAvg[op/s]")), EXPECTED_RATE
		);

		// metrics log file
		final List<CSVRecord> metricsLogRecs = getMetricsLogRecords();
		testMetricsLogRecords(
			metricsLogRecs, IoType.DELETE, concurrency.getValue(), driverCount.getValue(),
			new SizeInBytes(0), EXPECTED_COUNT, 0,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		/*for(final CSVRecord metricsLogRec : metricsLogRecs) {
			assertEquals(
				EXPECTED_RATE, Double.parseDouble(metricsLogRec.get("TPAvg[op/s]")),
				EXPECTED_RATE
			);
		}*/

		// test std output
		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.DELETE, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), EXPECTED_COUNT,
			new HashMap<IoType, Integer>() {{ put(IoType.DELETE, concurrency.getValue()); }}
		);
	}
}
