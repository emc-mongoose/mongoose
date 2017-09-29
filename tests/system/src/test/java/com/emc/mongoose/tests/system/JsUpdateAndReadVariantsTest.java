package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.EnvUtil;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import com.github.akurilov.commons.system.SizeInBytes;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JsUpdateAndReadVariantsTest
extends ScenarioTestBase {

	private static final int FIRST_STEP_COUNT_LIMIT = 100_000;

	private int containerExitCode;
	private String stdOutput;

	public JsUpdateAndReadVariantsTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before @Override
	public void setUp()
	throws Exception {

		super.setUp();

		switch(storageType) {
			case FS:
				final Path containerItemOutputPath = Paths.get(CONTAINER_SHARE_PATH, stepId);
				configArgs.add("--item-output-path=" + containerItemOutputPath.toString());
				break;
			case SWIFT:
				configArgs.add("--storage-net-http-namespace=ns1");
				break;
		}

		EnvUtil.set("ITEMS_FILE_0", stepId + "_0.csv");
		EnvUtil.set("ITEMS_FILE_1", stepId + "_1.csv");
		EnvUtil.set("FIRST_STEP_COUNT_LIMIT", Integer.toString(FIRST_STEP_COUNT_LIMIT));

		initTestContainer();
		dockerClient.startContainerCmd(testContainerId).exec();
		containerExitCode = dockerClient
			.waitContainerCmd(testContainerId)
			.exec(new WaitContainerResultCallback())
			.awaitStatusCode(300, TimeUnit.SECONDS);
		stdOutput = stdOutBuff.toString();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			DIR_EXAMPLE_SCENARIO, "js", "types", "additional", "update_and_read_variants.js"
		);
	}

	@Override
	public void test()
	throws Exception {

		final List<CSVRecord> totalMetricsRecs = getContainerMetricsTotalLogRecords();
		assertEquals(totalMetricsRecs.size(), 4);
		final SizeInBytes singleRangeSize = new SizeInBytes(1, itemSize.getValue().get(), 1);

		testTotalMetricsLogRecord(
			totalMetricsRecs.get(0), IoType.UPDATE, concurrency.getValue(), driverCount.getValue(),
			singleRangeSize, 0, FIRST_STEP_COUNT_LIMIT
		);

		assertEquals(IoType.READ.name(), totalMetricsRecs.get(1).get("TypeLoad"));
		final int succCount = Integer.parseInt(totalMetricsRecs.get(1).get("CountSucc"));
		assertEquals(0, succCount, FIRST_STEP_COUNT_LIMIT / 100);
		final int failCount = Integer.parseInt(totalMetricsRecs.get(1).get("CountFail"));
		assertEquals(FIRST_STEP_COUNT_LIMIT, failCount, FIRST_STEP_COUNT_LIMIT / 100);

		testTotalMetricsLogRecord(
			totalMetricsRecs.get(2), IoType.READ, concurrency.getValue(), driverCount.getValue(),
			singleRangeSize, 0, FIRST_STEP_COUNT_LIMIT
		);
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(3), IoType.READ, concurrency.getValue(), driverCount.getValue(),
			singleRangeSize, 0, FIRST_STEP_COUNT_LIMIT
		);

		assertEquals(0, containerExitCode);
	}
}
