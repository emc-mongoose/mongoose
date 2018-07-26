package com.emc.mongoose.system;

import com.emc.mongoose.config.BundledDefaultsProvider;
import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.*;
import com.emc.mongoose.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseSlaveNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.config.CliArgUtil.ARG_PATH_SEP;
import static com.emc.mongoose.system.util.LogValidationUtil.*;
import static com.emc.mongoose.system.util.TestCaseUtil.stepId;
import static com.emc.mongoose.system.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.system.util.docker.MongooseContainer.containerScenarioPath;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ReadVerificationAfterCircularUpdateTest {

    @Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
    public static List<Object[]> envParams() {
        return EnvParams.PARAMS;
    }

    private final int timeoutInMillis = 1000_000;
    private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
    private final Map<String, MongooseSlaveNodeContainer> slaveNodes = new HashMap<>();
    private final MongooseContainer testContainer;
    private final String stepId;
    private final StorageType storageType;
    private final RunMode runMode;
    private final Concurrency concurrency;
    private final ItemSize itemSize;
    private final Config config;
    private final String containerItemOutputPath;
    private final String hostItemOutputFile = HOST_SHARE_PATH + File.separator
            + CreateLimitBySizeTest.class.getSimpleName() + ".csv";
    private final int averagePeriod;

    private String stdOutContent = null;

    public ReadVerificationAfterCircularUpdateTest(
            final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
            final ItemSize itemSize
    ) throws Exception {

        final Map<String, Object> schema = SchemaProvider.resolveAndReduce(
                APP_NAME, Thread.currentThread().getContextClassLoader());
        config = new BundledDefaultsProvider().config(ARG_PATH_SEP, schema);

        final Object avgPeriodRaw = config.val("output-metrics-average-period");
        if (avgPeriodRaw instanceof String) {
            averagePeriod = (int) TimeUtil.getTimeInSeconds((String) avgPeriodRaw);
        } else {
            averagePeriod = TypeUtil.typeConvert(avgPeriodRaw, int.class);
        }

        stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
        containerItemOutputPath = MongooseContainer.getContainerItemOutputPath(stepId);

        try {
	        FileUtils.deleteDirectory(Paths.get(MongooseContainer.HOST_LOG_PATH.toString(), stepId).toFile());
        } catch (final IOException ignored) {
        }

        this.storageType = storageType;
        this.runMode = runMode;
        this.concurrency = concurrency;
        this.itemSize = itemSize;

        if (storageType.equals(StorageType.FS)) {
            try {
                DirWithManyFilesDeleter.deleteExternal(containerItemOutputPath);
            } catch (final Exception e) {
                e.printStackTrace(System.err);
            }
        }
        try {
            Files.delete(Paths.get(hostItemOutputFile));
        } catch (final Exception ignored) {
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
        testContainer.await(timeoutInMillis, TimeUnit.MILLISECONDS);
        stdOutContent = testContainer.stdOutContent();
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

        final LongAdder ioTraceRecCount = new LongAdder();
        final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
            assertEquals(
                    "Record #" + ioTraceRecCount.sum() + ": unexpected operation type " +
                            ioTraceRec.get("OpTypeCode"),
                    OpType.READ, OpType.values()[Integer.parseInt(ioTraceRec.get("OpTypeCode"))]
            );
            assertEquals(
                    "Record #" + ioTraceRecCount.sum() + ": unexpected status code " +
                            ioTraceRec.get("StatusCode"),
                    Operation.Status.SUCC,
                    Operation.Status.values()[Integer.parseInt(ioTraceRec.get("StatusCode"))]
            );
        };
        testIoTraceLogRecords(stepId, ioTraceReqTestFunc);

        testTotalMetricsLogRecord(
                getMetricsTotalLogRecords(stepId).get(0),
                OpType.READ, concurrency.getValue(), runMode.getNodeCount(), itemSize.getValue(), 0, 0
        );

        testMetricsLogRecords(
                getMetricsLogRecords(stepId),
                OpType.READ, concurrency.getValue(), runMode.getNodeCount(), itemSize.getValue(), 0, 0,
                averagePeriod
        );

        testSingleMetricsStdout(
                stdOutContent.replaceAll("[\r\n]+", " "),
                OpType.READ, concurrency.getValue(), runMode.getNodeCount(), itemSize.getValue(),
                averagePeriod
        );
        testMetricsTableStdout(
                stdOutContent, stepId, storageType, runMode.getNodeCount(), 0,
                new HashMap<OpType, Integer>() {{
                    put(OpType.CREATE, concurrency.getValue());
                    put(OpType.UPDATE, concurrency.getValue());
                    put(OpType.READ, concurrency.getValue());
                }}
        );
    }
}
