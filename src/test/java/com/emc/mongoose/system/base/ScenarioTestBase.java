package com.emc.mongoose.system.base;

import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.NodeCount;
import com.emc.mongoose.system.base.params.StorageType;
import com.emc.mongoose.system.util.docker.ContainerOutputCallback;

import static com.emc.mongoose.Constants.DIR_EXAMPLE_SCENARIO;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.PullImageResultCallback;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 Created by andrey on 23.09.17.
 */
public abstract class ScenarioTestBase
extends ContainerizedStorageTestBase {

	protected static final String BASE_DIR = new File("").getAbsolutePath();
	private static final Path DEFAULT_SCENARIO_PATH = Paths.get(
		BASE_DIR, DIR_EXAMPLE_SCENARIO, "js", "default.js"
	);
	protected static final String CONTAINER_HOME_PATH = "/root/.mongoose/" + APP_VERSION;
	protected static final String CONTAINER_SHARE_PATH = CONTAINER_HOME_PATH + "/share";
	protected final static Path HOST_SHARE_PATH = Paths.get(BASE_DIR, "share");
	static {
		HOST_SHARE_PATH.toFile().mkdir();
	}
	protected final static String CONTAINER_LOG_PATH = CONTAINER_HOME_PATH + "/log";
	protected final static Path HOST_LOG_PATH = Paths.get(BASE_DIR, "share", "log");
	static {
		HOST_LOG_PATH.toFile().mkdir();
	}

	private static final String
		BASE_SCRIPTING_IMAGE_NAME = "emcmongoose/mongoose:" + MONGOOSE_VERSION;
	private static final String
		GROOVY_SCRIPTING_ENGINE_IMAGE_NAME = "emcmongoose/mongoose-scripting-groovy:" +
		MONGOOSE_VERSION;
	private static final String
		JYTHON_SCRIPTING_ENGINE_IMAGE_NAME = "emcmongoose/mongoose-scripting-jython:" +
		MONGOOSE_VERSION;
	protected static final Map<String, String> SCENARIO_LANG_IMAGES = new HashMap<>();
	static {
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

	protected ScenarioTestBase(
		final StorageType storageType, final NodeCount nodeCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, nodeCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {

		super.setUp();

		scenarioPath = makeScenarioPath();
		if(scenarioPath == null) {
			scenarioPath = DEFAULT_SCENARIO_PATH;
		} else {
			final String scenarioPathStr = scenarioPath.toString();
			if(scenarioPathStr.startsWith(BASE_DIR)) {
				configArgs.add(
					"--run-scenario=" + APP_HOME_DIR + "/" + DIR_EXAMPLE_SCENARIO + "/"
						+ scenarioPathStr.substring(BASE_DIR.length())
				);
			} else {
				configArgs.add(
					"--run-scenario="  + APP_HOME_DIR + "/" + DIR_EXAMPLE_SCENARIO + "/"
						+ scenarioPathStr
				);
			}
		}
	}

	protected void initTestContainer()
	throws Exception {

		final String scenarioFileName = scenarioPath.getFileName().toString();
		int dotPos = scenarioFileName.lastIndexOf('.');
		if(dotPos > 0) {

			final String scenarioFileExt = scenarioFileName.substring(dotPos + 1);
			final String dockerImageName = SCENARIO_LANG_IMAGES.get(scenarioFileExt);
			if(dockerImageName == null) {
				fail();
			}
			System.out.println("docker pull " + dockerImageName + "...");
			dockerClient.pullImageCmd(dockerImageName)
				.exec(new PullImageResultCallback())
				.awaitCompletion();

			System.out.println("Container arguments: " + Arrays.toString(configArgs.toArray()));

			final Volume volumeShare = new Volume(CONTAINER_SHARE_PATH);
			final Volume volumeLog = new Volume(CONTAINER_LOG_PATH);
			final Bind[] binds = new Bind[] {
				new Bind(HOST_SHARE_PATH.toString(), volumeShare),
				new Bind(HOST_LOG_PATH.toString(), volumeLog),
			};

			// put the environment variables into the container
			final Map<String, String> envMap = System.getenv();
			final String[] env = envMap.keySet().toArray(new String[envMap.size()]);
			for(int i = 0; i < env.length; i ++) {
				if("PATH".equals(env[i])) {
					env[i] = env[i] + "=" + envMap.get(env[i]) + ":/bin";
				} else {
					env[i] = env[i] + "=" + envMap.get(env[i]);
				}
			}

			final CreateContainerResponse container = dockerClient
				.createContainerCmd(dockerImageName)
				.withName("mongoose")
				.withNetworkMode("host")
				.withExposedPorts(ExposedPort.tcp(9010), ExposedPort.tcp(5005))
				.withVolumes(volumeShare, volumeLog)
				.withBinds(binds)
				.withAttachStdout(true)
				.withAttachStderr(true)
				.withEnv(env)
				.withEntrypoint("/opt/mongoose/entrypoint-debug.sh")
				.withCmd(configArgs)
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
