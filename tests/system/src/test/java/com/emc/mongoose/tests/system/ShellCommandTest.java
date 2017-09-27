package com.emc.mongoose.tests.system;

import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.api.common.env.PathUtil.BASE_DIR;

import com.github.dockerjava.core.command.WaitContainerResultCallback;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ShellCommandTest
extends ScenarioTestBase {

	private String stdOutput;

	public ShellCommandTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			BASE_DIR, DIR_EXAMPLE_SCENARIO, "groovy", "systest", "ShellCommand.groovy"
		);
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		initTestContainer();
		dockerClient.startContainerCmd(testContainerId).exec();
		dockerClient
			.waitContainerCmd(testContainerId)
			.exec(new WaitContainerResultCallback())
			.awaitStatusCode(1000, TimeUnit.SECONDS);
		stdOutput = stdOutBuff.toString();
	}

	@After
	public final void tearDown()
	throws Exception {
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {
		assertTrue(stdOutput.contains("Hello world!"));
	}
}
