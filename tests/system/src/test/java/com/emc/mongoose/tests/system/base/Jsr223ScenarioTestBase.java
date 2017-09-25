package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;

import com.emc.mongoose.tests.system.util.docker.ContainerOutputCallback;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 Created by andrey on 23.09.17.
 */
public abstract class Jsr223ScenarioTestBase
extends ContainerizedStorageTestBase {

	private static final Path DEFAULT_SCENARIO_PATH = Paths.get(
		getBaseDir(), DIR_EXAMPLE_SCENARIO, "js", "default.js"
	);
	protected static Volume VOLUME_LOGS = new Volume("/opt/mongoose/log");

	private static final String
		BASE_SCRIPTING_IMAGE_NAME = "emcmongoose/mongoose:" + TEST_VERSION;
	private static final String
		GROOVY_SCRIPTING_ENGINE_IMAGE_NAME = "emcmongoose/mongoose-scripting-groovy:" + TEST_VERSION;
	private static final String
		JYTHON_SCRIPTING_ENGINE_IMAGE_NAME = "emcmongoose/mongoose-scripting-jython:" + TEST_VERSION;
	protected static final Map<String, String> SCENARIO_LANG_IMAGES = new HashMap<>();
	static {
		SCENARIO_LANG_IMAGES.put("json", BASE_SCRIPTING_IMAGE_NAME);
		SCENARIO_LANG_IMAGES.put("js", BASE_SCRIPTING_IMAGE_NAME);
		SCENARIO_LANG_IMAGES.put("groovy", GROOVY_SCRIPTING_ENGINE_IMAGE_NAME);
		SCENARIO_LANG_IMAGES.put("py", JYTHON_SCRIPTING_ENGINE_IMAGE_NAME);
	}

	protected Path scenarioPath = null;
	protected String testContainerId = null;

	protected final StringBuilder stdOutBuff = new StringBuilder();
	protected final StringBuilder stdErrBuff = new StringBuilder();
	private final ResultCallback<Frame> streamsCallback = new ContainerOutputCallback(
		stdOutBuff, stdErrBuff
	);

	protected Jsr223ScenarioTestBase(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {

		super.setUp();

		scenarioPath = makeScenarioPath();
		if(scenarioPath == null) {
			final String scenarioValue = config.getTestConfig().getScenarioConfig().getFile();
			if(scenarioValue != null && !scenarioValue.isEmpty()) {
				scenarioPath = Paths.get(scenarioValue);
				configArgs.add("--test-scenario-file=" + scenarioPath.toString());
			} else {
				scenarioPath = DEFAULT_SCENARIO_PATH;
			}
		} else {
			configArgs.add("--test-scenario-file=" + scenarioPath.toString());
		}

		final String scenarioFileName = scenarioPath.getFileName().toString();
		int dotPos = scenarioFileName.lastIndexOf('.');
		if(dotPos > 0) {

			final String scenarioFileExt = scenarioFileName.substring(dotPos + 1);
			final String dockerImageName = SCENARIO_LANG_IMAGES.get(scenarioFileExt);
			if(dockerImageName == null) {
				fail();
			}

			final List<String> cmd = new ArrayList<>();
			cmd.add("-Xms1g");
			cmd.add("-Xmx1g");
			cmd.add("-XX:MaxDirectMemorySize=1g");
			cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005");
			cmd.add("-Dcom.sun.management.jmxremote=true");
			cmd.add("-Dcom.sun.management.jmxremote.port=9010");
			cmd.add("-Dcom.sun.management.jmxremote.rmi.port=9010");
			cmd.add("-Dcom.sun.management.jmxremote.local.only=false");
			cmd.add("-Dcom.sun.management.jmxremote.authenticate=false");
			cmd.add("-Dcom.sun.management.jmxremote.ssl=false");
			cmd.add("-jar");
			cmd.add("/opt/mongoose/mongoose.jar");
			cmd.addAll(configArgs);

			final CreateContainerResponse container = dockerClient
				.createContainerCmd(dockerImageName)
				.withName("mongoose")
				.withNetworkMode("host")
				.withExposedPorts(ExposedPort.tcp(9010), ExposedPort.tcp(5005))
				.withVolumes(VOLUME_LOGS)
				.withBinds(new Bind(Paths.get(PathUtil.getBaseDir(), "log").toString(), VOLUME_LOGS))
				.withAttachStdout(true)
				.withAttachStderr(true)
				.withEntrypoint("java")
				.withCmd(cmd)
				.exec();

			testContainerId = container.getId();

			dockerClient
				.attachContainerCmd(testContainerId)
				.withStdErr(true)
				.withStdOut(true)
				.withFollowStream(true)
				.exec(streamsCallback);
		} else {
			fail();
		}

	}

	@After
	public void tearDown()
	throws Exception {
		streamsCallback.close();
		if(testContainerId != null) {
			dockerClient
				.removeContainerCmd(testContainerId)
				.withForce(true)
				.exec();
			testContainerId = null;
		}
		super.tearDown();
	}

	protected abstract Path makeScenarioPath();
}
