package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.EnvUtil;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;

import com.github.dockerjava.core.command.WaitContainerResultCallback;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 27.03.17.
 Covered use cases:
 * 2.1.1.1.2. Small Data Items (1B-100KB)
 * 2.2.1. Items Input File
 * 2.3.2. Items Output File
 * 4.3. Medium Concurrency Level (11-100)
 * 5. Circularity
 * 6.2.2. Limit Step by Processed Item Count
 * 6.2.5. Limit Step by Time
 * 7.1. Metrics Periodic Reporting
 * 7.2. Metrics Reporting is Suppressed for the Precondition Steps
 * 8.2.1. Create New Items
 * 8.3.1. Read With Disabled Validation
 * 9.1. Scenarios Syntax
 * 9.3. Custom Scenario File
 * 9.4.1. Override Default Configuration in the Scenario
 * 9.4.2. Step Configuration Inheritance
 * 9.4.3. Reusing The Items in the Scenario
 * 9.5.7.2. Weighted Load Step
 * 10.1.2. Many Local Separate Storage Driver Services (at different ports)
 */

public class PyWeightedLoadTest
extends ScenarioTestBase {

	private String stdOutput;
	private String itemOutputPath;
	private long duration;
	private int containerExitCode;

	public PyWeightedLoadTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "py", "types", "weighted.py");
	}

	@Override
	protected String makeStepId() {
		return PyWeightedLoadTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {
		configArgs.add("--storage-net-http-namespace=ns1");
		configArgs.add("--storage-mock-capacity=10000000");
		super.setUp();
		if(storageType.equals(StorageType.FS)) {
			itemOutputPath = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
			).toString();
			EnvUtil.set("ITEM_OUTPUT_PATH", itemOutputPath);
		} else {
			itemOutputPath = "/default";
			EnvUtil.set("ITEM_OUTPUT_PATH", stepId);
		}
		configArgs.add("--item-output-path=" + itemOutputPath);

		initTestContainer();
		duration = System.currentTimeMillis();
		dockerClient.startContainerCmd(testContainerId).exec();
		containerExitCode = dockerClient
			.waitContainerCmd(testContainerId)
			.exec(new WaitContainerResultCallback())
			.awaitStatusCode(2, TimeUnit.MINUTES);
		duration = System.currentTimeMillis() - duration;
		stdOutput = stdOutBuff.toString();
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

	@Override
	public void test()
	throws Exception {

		final Map<IoType, Integer> concurrencyMap = new HashMap<>();
		concurrencyMap.put(IoType.CREATE, concurrency.getValue());
		concurrencyMap.put(IoType.READ, concurrency.getValue());
		testMetricsTableStdout(stdOutput, stepId, driverCount.getValue(), 0, concurrencyMap);
		assertTrue(
			"Scenario didn't finished in time, actual duration: " + duration / 1000,
			duration <= 120_000
		);
	}
}
