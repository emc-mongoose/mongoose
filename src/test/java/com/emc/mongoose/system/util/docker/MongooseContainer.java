package com.emc.mongoose.system.util.docker;

import com.emc.mongoose.config.BundledDefaultsProvider;
import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.Constants.USER_HOME;
import static com.emc.mongoose.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.system.util.TestCaseUtil.snakeCaseName;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MongooseContainer
        extends ContainerBase {

    public static final String IMAGE_VERSION = System.getenv("MONGOOSE_VERSION");

    public static final Config BUNDLED_DEFAULTS;

    static {
        try {
            BUNDLED_DEFAULTS = new BundledDefaultsProvider()
                    .config(
                            ARG_PATH_SEP,
                            SchemaProvider.resolveAndReduce(
                                    APP_NAME, Thread.currentThread().getContextClassLoader()
                            )
                    );
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    public static final String APP_VERSION = BUNDLED_DEFAULTS.stringVal("run-version");
    public static final String APP_HOME_DIR = Paths
            .get(USER_HOME, "." + APP_NAME, APP_VERSION)
            .toString();
    public static final String CONTAINER_HOME_PATH = "/root/.mongoose/" + APP_VERSION;

    private static final String IMAGE_NAME = "emcmongoose/mongoose";
    private static final String ENTRYPOINT = "/opt/mongoose/entrypoint.sh";
    private static final String ENTRYPOINT_DEBUG = "/opt/mongoose/entrypoint.sh";
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

    public static String containerScenarioPath(final Class testCaseCls) {
        return CONTAINER_HOME_PATH + "/" + DIR_EXAMPLE_SCENARIO + "/js/systest/"
                + snakeCaseName(testCaseCls) + ".js";
    }

    private final List<String> args;

    private String containerItemOutputPath = null;
    private String hostItemOutputPath = null;

    public static String getContainerItemOutputPath(final String stepId) {
        return Paths.get(CONTAINER_SHARE_PATH, stepId).toString();
    }

    public static String getHostItemOutputPath(final String stepId) {
        return Paths.get(HOST_SHARE_PATH.toString(), stepId).toString();
    }

    public MongooseContainer(
            final String stepId, final StorageType storageType,
            final RunMode runMode, final Concurrency concurrency, final ItemSize itemSize,
            final String containerScenarioPath, final List<String> env, final List<String> args
    ) throws InterruptedException {
        this(
                IMAGE_VERSION, stepId, storageType, runMode, concurrency, itemSize,
                containerScenarioPath, env, args
        );
    }

    public MongooseContainer(
            final String version, final String stepId, final StorageType storageType,
            final RunMode runMode, final Concurrency concurrency, final ItemSize itemSize,
            final String containerScenarioPath, final List<String> env, final List<String> args
    ) throws InterruptedException {
        super(version, env, VOLUME_BINDS, true, PORT_DEBUG, PORT_JMX);
        this.args = args;
        this.args.add("--load-step-id=" + stepId);
        this.args.add("--load-step-limit-concurrency=" + concurrency.getValue());
        this.args.add("--item-data-size=" + itemSize.getValue());
        this.args.add("--output-metrics-trace-persist");
        if (containerScenarioPath != null) {
            this.args.add("--run-scenario=" + containerScenarioPath);
        }
        this.args.add("--storage-driver-type=" + storageType.name().toLowerCase());
        switch (storageType) {
            case S3:
                break;
            case ATMOS:
                break;
            case FS:
                containerItemOutputPath = getContainerItemOutputPath(stepId);
                hostItemOutputPath = getHostItemOutputPath(stepId);
                args.add("--item-output-path=" + containerItemOutputPath);
                break;
            case SWIFT:
                args.add("--storage-net-http-namespace=ns1");
                break;
        }
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
