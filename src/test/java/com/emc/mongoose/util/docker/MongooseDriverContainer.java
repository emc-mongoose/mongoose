package com.emc.mongoose.util.docker;

import com.emc.mongoose.config.BundledDefaultsProvider;
import com.emc.mongoose.params.Concurrency;
import com.emc.mongoose.params.RunMode;
import com.emc.mongoose.params.StorageType;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.dockerjava.core.command.BuildImageResultCallback;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.Constants.M;
import static com.emc.mongoose.Constants.USER_HOME;
import static com.emc.mongoose.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.util.TestCaseUtil.snakeCaseName;

public final class MongooseDriverContainer
	extends ContainerBase {

	public static final String IMAGE_VERSION = MongooseContainer.IMAGE_VERSION;
	public static final String APP_VERSION = MongooseContainer.APP_VERSION;
	public static final String APP_HOME_DIR = MongooseContainer.APP_HOME_DIR;
	public static final String BASE_DIR = new File("").getAbsolutePath();
	public static final String CONTAINER_HOME_PATH = MongooseContainer.CONTAINER_HOME_PATH;
	private static String IMAGE_NAME;
	private static final String ENTRYPOINT = "/opt/mongoose/entrypoint.sh";
	private static final String ENTRYPOINT_DEBUG = "/opt/mongoose/entrypoint-debug.sh";
	private static final int PORT_DEBUG = 5005;
	private static final int PORT_JMX = 9010;
	public static final String CONTAINER_SHARE_PATH = CONTAINER_HOME_PATH + "/share";
	public static final Path HOST_SHARE_PATH = Paths.get(APP_HOME_DIR, "share");

	static {
		HOST_SHARE_PATH.toFile().mkdir();
	}

	private static final String CONTAINER_LOG_PATH = CONTAINER_HOME_PATH + "/log";
	public static final Path HOST_LOG_PATH = Paths.get(APP_HOME_DIR, "log");

	static {
		HOST_LOG_PATH.toFile().mkdir();
	}

	private static final Map<String, Path> VOLUME_BINDS = new HashMap<String, Path>() {{
		put(CONTAINER_LOG_PATH, HOST_LOG_PATH);
		put(CONTAINER_SHARE_PATH, HOST_SHARE_PATH);
	}};

	public static String systemTestContainerScenarioPath(final Class testCaseCls) {
		return MongooseContainer.systemTestContainerScenarioPath(testCaseCls);
	}

	public static String enduranceTestContainerScenarioPath(final Class testCaseCls) {
		return MongooseContainer.enduranceTestContainerScenarioPath(testCaseCls);
	}

	private final List<String> args;
	private String containerItemOutputPath = null;
	private String hostItemOutputPath = null;

	public static String getContainerItemOutputPath(final String stepId) {
		return MongooseContainer.getContainerItemOutputPath(stepId);
	}

	public static String getHostItemOutputPath(final String stepId) {
		return MongooseContainer.getHostItemOutputPath(stepId);
	}

	public MongooseDriverContainer(
		final String imageName, final String stepId, final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
		final SizeInBytes itemSize, final String containerScenarioPath, final List<String> env, final List<String> args
	)
	throws InterruptedException {
		this(imageName, stepId, storageType, runMode, concurrency, itemSize, containerScenarioPath, env, args, true, true, true);
	}

	public MongooseDriverContainer(
		final String imageName, final String stepId, final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
		final SizeInBytes itemSize, final String containerScenarioPath, final List<String> env, final List<String> args,
		final boolean attachOutputFlag, final boolean collectOutputFlag, final boolean outputMetricsTracePersistFlag
	)
	throws InterruptedException {
		this(
			imageName, IMAGE_VERSION, stepId, storageType, runMode, concurrency, itemSize, containerScenarioPath, env, args,
			attachOutputFlag, collectOutputFlag, outputMetricsTracePersistFlag
		);
	}

	public MongooseDriverContainer(
		final String imageName, final String version, final String stepId, final String containerScenarioPath,
		final List<String> env, final List<String> args, final boolean attachOutputFlag,
		final boolean collectOutputFlag, final boolean outputMetricsTracePersistFlag
	)
	throws InterruptedException {
		super(version, env, VOLUME_BINDS, attachOutputFlag, collectOutputFlag, PORT_DEBUG, PORT_JMX);
		this.args = args;
		this.args.add("--load-step-id=" + stepId);
		if(outputMetricsTracePersistFlag) {
			this.args.add("--output-metrics-trace-persist");
		}
		if(containerScenarioPath != null) {
			this.args.add("--run-scenario=" + containerScenarioPath);
		}
		buildImage(imageName);
	}

	public MongooseDriverContainer(
		final String imageName, final String version, final String stepId, final StorageType storageType, final RunMode runMode,
		final Concurrency concurrency, final SizeInBytes itemSize, final String containerScenarioPath,
		final List<String> env, final List<String> args, final boolean attachOutputFlag,
		final boolean collectOutputFlag, final boolean outputMetricsTracePersistFlag
	)
	throws InterruptedException {
		this(imageName, version, stepId, containerScenarioPath, env, args, attachOutputFlag, collectOutputFlag,
			outputMetricsTracePersistFlag);
		this.args.add("--storage-driver-limit-concurrency=" + concurrency.getValue());
		this.args.add("--item-data-size=" + itemSize);
		this.args.add("--storage-driver-type=" + storageType.name().toLowerCase());
		switch(storageType) {
			case S3:
				break;
			case ATMOS:
				break;
			case FS:
				containerItemOutputPath = getContainerItemOutputPath(stepId);
				hostItemOutputPath = getHostItemOutputPath(stepId);
				if(args.stream().noneMatch(arg -> arg.startsWith("--item-output-path="))) {
					args.add("--item-output-path=" + containerItemOutputPath);
				}
				break;
			case SWIFT:
				args.add("--storage-net-http-namespace=ns1");
				break;
		}
	}

	public static final String buildImage(final String tag){
		final File dockerBuildFile = Paths
			.get(BASE_DIR, "docker", "Dockerfile")
			.toFile();
		final BuildImageResultCallback buildImageResultCallback = new BuildImageResultCallback();
		Docker.CLIENT
			.buildImageCmd()
			.withBaseDirectory(new File(BASE_DIR))
			.withDockerfile(dockerBuildFile)
			.withBuildArg("MONGOOSE_VERSION", APP_VERSION)
			.withPull(true)
			.withTags(Collections.singleton(tag))
			.exec(buildImageResultCallback);
		return buildImageResultCallback.awaitImageId();
	}

	public final void imageName(final String imageName){
		IMAGE_NAME = imageName;
	}

	@Override
	protected final String imageName() {
		return IMAGE_NAME;
	}

	@Override
	protected final List<String> containerArgs() {
		return args;
	}

	@Override
	protected final String entrypoint() {
		return ENTRYPOINT;
	}
}
