package com.emc.mongoose.system;

import com.emc.mongoose.config.ConfigUtil;
import com.emc.mongoose.env.MainInstaller;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.*;
import com.emc.mongoose.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.system.util.OpenFilesCounter;
import com.emc.mongoose.system.util.PortTools;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseSlaveNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.emc.mongoose.system.util.LogValidationUtil.testMetricsTableStdout;
import static com.emc.mongoose.system.util.TestCaseUtil.stepId;
import static com.emc.mongoose.system.util.docker.MongooseContainer.containerScenarioPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class UnlimitedCreateTest {

    @Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
    public static List<Object[]> envParams() {
        return EnvParams.PARAMS;
    }

    //private final String SCENARIO_PATH = Paths.get("", "example", "scenarios", "js", "default.js").toString();
    private final String itemOutputPath;

    private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
    private final Map<String, MongooseSlaveNodeContainer> slaveNodes = new HashMap<>();
    private final MongooseContainer testContainer;
    private final String stepId;
    private final StorageType storageType;
    private final RunMode runMode;
    private final Concurrency concurrency;
    private final ItemSize itemSize;

    public UnlimitedCreateTest(
            final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
            final ItemSize itemSize
    ) throws Exception {

        stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
        itemOutputPath = Paths.get("", stepId).toString();

        try {
            FileUtils.deleteDirectory(MongooseContainer.HOST_LOG_PATH.toFile());
        } catch (final IOException ignored) {
        }

        this.storageType = storageType;
        this.runMode = runMode;
        //in unlimited test next params are unnecessary
        this.concurrency = concurrency;
        this.itemSize = itemSize;

        if (storageType.equals(StorageType.FS)) {
            try {
                DirWithManyFilesDeleter.deleteExternal(itemOutputPath);
            } catch (final Exception e) {
                e.printStackTrace(System.err);
            }
        }

        final List<String> env = System.getenv()
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList());

        final List<String> args = new ArrayList<>();

        switch (storageType) {
            case ATMOS:
            case S3:
            case SWIFT:
                final HttpStorageMockContainer storageMock = new HttpStorageMockContainer(
                        HttpStorageMockContainer.DEFAULT_PORT, false, null, null, Character.MAX_RADIX,
                        HttpStorageMockContainer.DEFAULT_CAPACITY,
                        HttpStorageMockContainer.DEFAULT_CONTAINER_CAPACITY,
                        HttpStorageMockContainer.DEFAULT_CONTAINER_COUNT_LIMIT,
                        HttpStorageMockContainer.DEFAULT_FAIL_CONNECT_EVERY,
                        HttpStorageMockContainer.DEFAULT_FAIL_RESPONSES_EVERY,
                        0
                );
                final String addr = "127.0.0.1:" + HttpStorageMockContainer.DEFAULT_PORT;
                storageMocks.put(addr, storageMock);
                args.add(
                        "--storage-net-node-addrs="
                                + storageMocks.keySet().stream().collect(Collectors.joining(","))
                );
                break;
        }

        switch (runMode) {
            case DISTRIBUTED:
                final String localExternalAddr = ServiceUtil.getAnyExternalHostAddress();
                for (int i = 1; i < runMode.getNodeCount(); i++) {
                    final int port = MongooseSlaveNodeContainer.DEFAULT_PORT + i;
                    final MongooseSlaveNodeContainer nodeSvc = new MongooseSlaveNodeContainer(port);
                    final String addr = localExternalAddr + ":" + port;
                    slaveNodes.put(addr, nodeSvc);
                }
                args.add(
                        "--load-step-node-addrs="
                                + slaveNodes.keySet().stream().collect(Collectors.joining(","))
                );
                break;
        }

        final String containerScenarioPath = containerScenarioPath(getClass());
        testContainer = new MongooseContainer(
                stepId, storageType, runMode, concurrency, itemSize, containerScenarioPath, env, args
        );
    }

    @Before
    public final void setUp()
            throws Exception {
        storageMocks.values().forEach(AsyncRunnableBase::start);
        slaveNodes.values().forEach(AsyncRunnableBase::start);
        testContainer.start();
        testContainer.await(60, TimeUnit.SECONDS);
    }

    @After
    public final void tearDown()
            throws Exception {

        testContainer.close();

        slaveNodes.values().parallelStream().forEach(
                storageMock -> {
                    try {
                        storageMock.close();
                    } catch (final Throwable t) {
                        t.printStackTrace(System.err);
                    }
                }
        );
        storageMocks.values().parallelStream().forEach(
                storageMock -> {
                    try {
                        storageMock.close();
                    } catch (final Throwable t) {
                        t.printStackTrace(System.err);
                    }
                }
        );
    }

    @Test
    public final void test()
            throws Exception {

        final String stdOutContent = testContainer.stdOutContent();

        testMetricsTableStdout(
                stdOutContent, stepId, storageType, runMode.getNodeCount(), 0,
                new HashMap<IoType, Integer>() {{
                    put(IoType.CREATE, concurrency.getValue());
                }}
        );

        final int expectedConcurrency = runMode.getNodeCount() * concurrency.getValue();
        if (storageType.equals(StorageType.FS)) {
            final int actualConcurrency = OpenFilesCounter.getOpenFilesCount(itemOutputPath);
            assertTrue(
                    "Expected concurrency <= " + actualConcurrency + ", actual: " + actualConcurrency,
                    actualConcurrency <= expectedConcurrency
            );
        } else {
            int actualConcurrency = 0;
            final int startPort = HttpStorageMockContainer.DEFAULT_PORT;
            for (int j = 0; j < runMode.getNodeCount(); ++j) {
                actualConcurrency += PortTools.getConnectionCount("127.0.0.1:" + (startPort + j));
            }
            assertEquals(
                    "Expected concurrency: " + actualConcurrency + ", actual: " + actualConcurrency,
                    expectedConcurrency, actualConcurrency, expectedConcurrency / 100
            );
        }
    }
}
