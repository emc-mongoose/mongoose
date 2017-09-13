package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.model.io.IoType.CREATE;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.akurilov.commons.system.SizeInBytes;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 Created by andrey on 25.08.17.
 */
public class UnlimitedConcurrencyLimitByRateTest
extends ScenarioTestBase {

	private static final int COUNT_LIMIT = 1_000_000;
	private static final SizeInBytes SIZE_LIMIT = new SizeInBytes("10GB");
	private static final int TIME_LIMIT_SEC = 60;
	private static final int RATE_LIMIT = 1000;

	private String itemOutputPath = null;
	private String stdOutput;
	private long runTime;

	public UnlimitedConcurrencyLimitByRateTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_SCENARIO, "systest", "UnlimitedConcurrencyLimitByRate.json"
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
				config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
				config.getTestConfig().getStepConfig().getLimitConfig().setSize(SIZE_LIMIT);
				break;
			case SWIFT:
				config.getStorageConfig().getNetConfig().getHttpConfig().setNamespace("ns1");
		}
		try {
			scenario = new JsonScenario(config, scenarioPath.toFile());
			stdOutStream.startRecording();
			runTime = System.currentTimeMillis();
			scenario.run();
			runTime = System.currentTimeMillis() - runTime;
			runTime = TimeUnit.MILLISECONDS.toSeconds(runTime);
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

		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), COUNT_LIMIT,
			new HashMap<IoType, Integer>() {{ put(CREATE, 0); }}
		);

		testSingleMetricsStdout(
			stdOutput, CREATE, 0, driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		final List<CSVRecord> metricsLogRecs = getMetricsLogRecords();
		testMetricsLogRecords(
			metricsLogRecs, CREATE, 0, driverCount.getValue(), itemSize.getValue(), COUNT_LIMIT,
			TIME_LIMIT_SEC,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		final List<CSVRecord> totalMetricsRecs = getMetricsTotalLogRecords();
		assertEquals(totalMetricsRecs.size(), 1);
		final CSVRecord totalMetricsRec = totalMetricsRecs.get(0);
		testTotalMetricsLogRecord(
			totalMetricsRec, CREATE, 0, driverCount.getValue(), itemSize.getValue(), COUNT_LIMIT,
			TIME_LIMIT_SEC
		);
		final double rate = Double.parseDouble(totalMetricsRec.get("TPAvg[op/s]"));
		assertTrue(rate < RATE_LIMIT + RATE_LIMIT / 2);
		final long totalSize = Long.parseLong(totalMetricsRec.get("Size"));
		if(StorageType.FS.equals(storageType)) {
			assertTrue(totalSize < SIZE_LIMIT.get() + SIZE_LIMIT.get() / 10);
		}

		assertTrue(
			"Test time was " + runTime + " while expected no more than " + TIME_LIMIT_SEC,
			TIME_LIMIT_SEC + 5 >= runTime
		);
	}
}
