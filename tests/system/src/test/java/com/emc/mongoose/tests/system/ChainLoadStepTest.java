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
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Before;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 09.06.17.
 */
public class ChainLoadStepTest
extends ScenarioTestBase {

	private static final long COUNT_LIMIT = 100_000;

	private String itemOutputPath;
	private String stdOutput;

	public ChainLoadStepTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {
		configArgs.add("--test-step-limit-count=" + COUNT_LIMIT);
		super.setUp();
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
		stdOutStream.startRecording();
		scenario.run();
		TimeUnit.SECONDS.sleep(10);
		LogUtil.flushAll();
		stdOutput = stdOutStream.stopRecordingAndGet();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "ChainStep.json");
	}

	@Override
	protected String makeStepId() {
		return ChainLoadStepTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	public final void test()
	throws Exception {
		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), COUNT_LIMIT,
			new HashMap<IoType, Integer>() {{
				put(IoType.CREATE, concurrency.getValue());
				put(IoType.READ, concurrency.getValue());
				put(IoType.UPDATE, concurrency.getValue());
				put(IoType.DELETE, concurrency.getValue());
				put(IoType.NOOP, concurrency.getValue());
			}}
		);
		final List<CSVRecord> totalRecs = getMetricsTotalLogRecords();
		testTotalMetricsLogRecord(
			totalRecs.get(0), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), COUNT_LIMIT, 0
		);
		testTotalMetricsLogRecord(
			totalRecs.get(1), IoType.READ, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), COUNT_LIMIT, 0
		);
		testTotalMetricsLogRecord(
			totalRecs.get(2), IoType.UPDATE, concurrency.getValue(), driverCount.getValue(),
			new SizeInBytes(1, itemSize.getValue().get(), 1), COUNT_LIMIT, 0
		);
		// looks like nagaina is not fast enough to reflect the immediate data changes...
		testTotalMetricsLogRecord(
			totalRecs.get(3), IoType.READ, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), 0, 0
		);
		testTotalMetricsLogRecord(
			totalRecs.get(4), IoType.DELETE, concurrency.getValue(), driverCount.getValue(),
			new SizeInBytes(0), 0, 0
		);
		testTotalMetricsLogRecord(
			totalRecs.get(5), IoType.NOOP, concurrency.getValue(), driverCount.getValue(),
			new SizeInBytes(0), 0, 0
		);
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
}
