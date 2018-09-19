package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.emc.mongoose.tests.system.util.EnvUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import org.junit.Before;

import com.github.dockerjava.core.command.WaitContainerResultCallback;

import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GroovyLoadTypeStepsTest
extends ScenarioTestBase {

	private static final int FIRST_STEP_DURATION_LIMIT = 20;
	private static final int UPDATE_RANDOM_RANGES_COUNT = 7;

	private int containerExitCode;
	private String stdOutput;

	public GroovyLoadTypeStepsTest(
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
		EnvUtil.set("ITEMS_FILE_2", stepId + "_2.csv");
		EnvUtil.set("FIRST_STEP_DURATION_LIMIT", Integer.toString(FIRST_STEP_DURATION_LIMIT));
		EnvUtil.set("UPDATE_RANDOM_RANGES_COUNT", Integer.toString(UPDATE_RANDOM_RANGES_COUNT));

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
		return Paths.get(DIR_EXAMPLE_SCENARIO, "groovy", "types", "additional", "load_type.groovy");
	}

	@Override
	public void test()
	throws Exception {

		final List<CSVRecord> totalMetricsRecs = getContainerMetricsTotalLogRecords();
		assertEquals(totalMetricsRecs.size(), 5);
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(0), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), 0, FIRST_STEP_DURATION_LIMIT
		);
		final long createdItemsCount = Long.parseLong(totalMetricsRecs.get(0).get("CountSucc"));
		assertTrue(createdItemsCount > 0);
		final SizeInBytes expectedUpdTransferAvgSize = new SizeInBytes(
			2 << (UPDATE_RANDOM_RANGES_COUNT - 2), itemSize.getValue().get(), 1
		);
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(1), IoType.UPDATE, concurrency.getValue(), driverCount.getValue(),
			expectedUpdTransferAvgSize, createdItemsCount, 0
		);
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(2), IoType.READ, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), createdItemsCount, 0
		);
		final SizeInBytes emptySize = new SizeInBytes(0);
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(3), IoType.DELETE, concurrency.getValue(), driverCount.getValue(),
			emptySize, createdItemsCount, 0
		);
		testTotalMetricsLogRecord(
			totalMetricsRecs.get(4), IoType.NOOP, concurrency.getValue(), driverCount.getValue(),
			emptySize, createdItemsCount, 0
		);

		assertEquals(0, containerExitCode);
	}
}
