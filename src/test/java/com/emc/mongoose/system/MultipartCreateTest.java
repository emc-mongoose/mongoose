package com.emc.mongoose.system;

import com.emc.mongoose.config.BundledDefaultsProvider;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.*;
import com.emc.mongoose.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseSlaveNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import static com.emc.mongoose.system.util.docker.MongooseContainer.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class MultipartCreateTest {

    @Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
    public static List<Object[]> envParams() {
        return EnvParams.PARAMS;
    }

    private final int timeoutInMillis = 1000_000;
    private final String itemOutputFile = MultipartCreateTest.class.getSimpleName() + "Items.csv";
    private final Map<String, HttpStorageMockContainer> storageMocks = new HashMap<>();
    private final Map<String, MongooseSlaveNodeContainer> slaveNodes = new HashMap<>();
    private final MongooseContainer testContainer;
    private final String stepId;
    private final StorageType storageType;
    private final RunMode runMode;
    private final Concurrency concurrency;
    private final ItemSize itemSize;
    private final Config config;
    private final String hostItemOutputFile = HOST_SHARE_PATH + File.separator
            + CreateLimitBySizeTest.class.getSimpleName() + ".csv";
    private final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");

    private int averagePeriod;
    private String stdOutContent = null;
    private String containerItemOutputPath;
    private long expectedCountMin;
    private long expectedCountMax;
    private SizeInBytes partSize;
    private SizeInBytes fullItemSize;
    private SizeInBytes sizeLimit;

    public MultipartCreateTest(
            final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
            final ItemSize itemSize
    ) throws Exception {

        partSize = itemSize.getValue();
        fullItemSize = new SizeInBytes(partSize.get(), 100 * partSize.get(), 3);

        Loggers.MSG.info("Item size: {}, part size: {}", fullItemSize, partSize);

        sizeLimit = new SizeInBytes(
                (runMode.getNodeCount() + 1) * concurrency.getValue() * fullItemSize.getAvg()
        );
        Loggers.MSG.info("Use the size limit: {}", sizeLimit);

        expectedCountMin = sizeLimit.get() / fullItemSize.getMax();
        expectedCountMax = sizeLimit.get() / fullItemSize.getMin();

        final Map<String, Object> schema = SchemaProvider.resolveAndReduce(
                APP_NAME, Thread.currentThread().getContextClassLoader());
        config = new BundledDefaultsProvider().config(ARG_PATH_SEP, schema);

        averagePeriod = config.intVal("output-metrics-average-period");

        stepId = stepId(getClass(), storageType, runMode, concurrency, itemSize);
        containerItemOutputPath = Paths.get(CONTAINER_SHARE_PATH, stepId).toString();

        try {
            FileUtils.deleteDirectory(MongooseContainer.HOST_LOG_PATH.toFile());
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
        env.add("PART_SIZE=" + partSize.toString());
        env.add("ITEM_OUTPUT_FILE=" + itemOutputFile);
        env.add("SIZE_LIMIT=" + sizeLimit.toString());

        final List<String> args = new ArrayList<>();
        args.add("--item-data-size=" + fullItemSize);

        switch (storageType) {
            case ATMOS:
            case S3:
            case SWIFT:
                final HttpStorageMockContainer storageMock = new HttpStorageMockContainer(
                        HttpStorageMockContainer.DEFAULT_PORT, false, null, null,
                        itemIdRadix,
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
        final SizeInBytes ZERO_SIZE = new SizeInBytes(0);
        final SizeInBytes TAIL_PART_SIZE = new SizeInBytes(1, partSize.get(), 1);
        final Consumer<CSVRecord> ioTraceRecFunc = ioTraceRec -> {
            try {
                testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), ZERO_SIZE);
            } catch (final AssertionError e) {
                try {
                    testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), partSize);
                } catch (final AssertionError ee) {
                    testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), TAIL_PART_SIZE);
                }
            }
            ioTraceRecCount.increment();
        };
        testIoTraceLogRecords(stepId, ioTraceRecFunc);

        final List<CSVRecord> itemRecs = new ArrayList<>();
        try (final BufferedReader br = new BufferedReader(new FileReader(itemOutputFile))) {
            try (final CSVParser csvParser = CSVFormat.RFC4180.parse(br)) {
                for (final CSVRecord csvRecord : csvParser) {
                    itemRecs.add(csvRecord);
                }
            }
        }
        long nextItemSize;
        long sizeSum = 0;
        final int n = itemRecs.size();
        assertTrue(n > 0);
        assertTrue(
                "Expected no less than " + expectedCountMin + " items, but got " + n,
                expectedCountMin <= n
        );
        assertTrue(
                "Expected no more than " + expectedCountMax + " items, but got " + n,
                expectedCountMax >= n
        );
        for (final CSVRecord itemRec : itemRecs) {
            nextItemSize = Long.parseLong(itemRec.get(2));
            assertTrue(fullItemSize.getMin() <= nextItemSize);
            assertTrue(fullItemSize.getMax() >= nextItemSize);
            sizeSum += nextItemSize;
        }
        final long delta =
                +runMode.getNodeCount() * concurrency.getValue() * partSize.getMax();
        System.out.println(
                "Expected transfer size: " + sizeLimit.get() + "+" + delta + ", actual: " + sizeSum
        );
        assertTrue(
                "Expected to transfer no more than " + sizeLimit + "+" + delta
                        + ", but transferred actually: " + new SizeInBytes(sizeSum),
                sizeLimit.get() + delta >= sizeSum
        );

        final List<CSVRecord> totalMetrcisLogRecords = getMetricsTotalLogRecords(stepId);
        assertEquals(
                "There should be 1 total metrics records in the log file", 1,
                totalMetrcisLogRecords.size()
        );
        testTotalMetricsLogRecord(
                totalMetrcisLogRecords.get(0), IoType.CREATE, concurrency.getValue(),
                runMode.getNodeCount(), fullItemSize, 0, 0
        );

        testSingleMetricsStdout(
                stdOutContent.replaceAll("[\r\n]+", " "),
                IoType.CREATE, concurrency.getValue(), runMode.getNodeCount(), fullItemSize,
                averagePeriod
        );
    }
}
