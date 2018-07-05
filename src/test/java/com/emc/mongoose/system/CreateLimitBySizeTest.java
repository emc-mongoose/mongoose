package com.emc.mongoose.system;

import com.emc.mongoose.config.BundledDefaultsProvider;
import com.emc.mongoose.config.ConfigUtil;
import com.emc.mongoose.config.TimeUtil;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.svc.ServiceUtil;
import com.emc.mongoose.system.base.params.*;
import com.emc.mongoose.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.system.util.HttpStorageMockUtil;
import com.emc.mongoose.system.util.docker.HttpStorageMockContainer;
import com.emc.mongoose.system.util.docker.MongooseContainer;
import com.emc.mongoose.system.util.docker.MongooseSlaveNodeContainer;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.dockerjava.api.exception.ConflictException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.Frequency;
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
public class CreateLimitBySizeTest {

    @Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
    public static List<Object[]> envParams() {
        return EnvParams.PARAMS;
    }


    private final float requiredAccuracy = 1 / 1000;
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
    private final String containerItemOutputFile = CONTAINER_SHARE_PATH + File.separator
            + CreateLimitBySizeTest.class.getSimpleName() + ".csv";
    private final String hostItemOutputFile = HOST_SHARE_PATH + File.separator
            + CreateLimitBySizeTest.class.getSimpleName() + ".csv";
    private final int itemIdRadix = BUNDLED_DEFAULTS.intVal("item-naming-radix");

    private SizeInBytes sizeLimit;
    private long expectedCount;
    private long duration;
    private int containerExitCode;
    private int averagePeriod;

    private String stdOutContent = null;
    private String containerItemOutputPath;
    private String hostItemOutputPath;

    public CreateLimitBySizeTest(
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
        hostItemOutputPath = MongooseContainer.getHostItemOutputPath(stepId);

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

        final List<String> args = new ArrayList<>();

        args.add("--item-output-file=" + containerItemOutputFile);
        args.add("--load-step-limit-size=" + sizeLimit);

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
                args.add("--storage-net-node-addrs="
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
                args.add("--load-step-node-addrs="
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

        //set-up a izeLimit depending on the itemSize
        final long itemSizeValue = itemSize.getValue().get();
        if (itemSizeValue > SizeInBytes.toFixedSize("1GB")) {
            sizeLimit = new SizeInBytes(100 * itemSizeValue);
        } else if (itemSizeValue > SizeInBytes.toFixedSize("1MB")) {
            sizeLimit = new SizeInBytes(1_000 * itemSizeValue);
        } else if (itemSizeValue > SizeInBytes.toFixedSize("10KB")) {
            sizeLimit = new SizeInBytes(10_000 * itemSizeValue);
        } else {
            sizeLimit = new SizeInBytes(100_000 * itemSizeValue);
        }
        expectedCount = sizeLimit.get() / itemSizeValue;

        storageMocks.values().forEach(AsyncRunnableBase::start);
        slaveNodes.values().forEach(AsyncRunnableBase::start);

        duration = System.currentTimeMillis();
        testContainer.start();
        testContainer.await(timeoutInMillis, TimeUnit.MILLISECONDS);
        duration = System.currentTimeMillis() - duration;

        containerExitCode = testContainer.exitStatusCode();
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

        assertEquals("Container exit code should be 0", 0, containerExitCode);

        final LongAdder ioTraceRecCount = new LongAdder();

        final Consumer<CSVRecord> ioTraceRecFunc;
        if (storageType.equals(StorageType.FS)) {
            ioTraceRecFunc = ioTraceRecord -> {
                File nextDstFile;
                final String nextItemPath = ioTraceRecord.get("ItemPath");
                final String nextItemId = nextItemPath.substring(
                        nextItemPath.lastIndexOf(File.separatorChar) + 1
                );
                nextDstFile = Paths.get(hostItemOutputPath, nextItemId).toFile();
                assertTrue("File \"" + nextDstFile + "\" doesn't exist", nextDstFile.exists());
                assertEquals(
                        "File (" + nextItemPath + ") size (" + nextDstFile.length() +
                                " is not equal to the configured: " + itemSize.getValue(),
                        itemSize.getValue().get(), nextDstFile.length()
                );
                ioTraceRecCount.increment();
            };
        } else {
            final String nodeAddr = storageMocks.keySet().iterator().next();
            ioTraceRecFunc = ioTraceRec -> {
                testIoTraceRecord(ioTraceRec, IoType.CREATE.ordinal(), itemSize.getValue());
                HttpStorageMockUtil.assertItemExists(
                        nodeAddr, ioTraceRec.get("ItemPath"),
                        Long.parseLong(ioTraceRec.get("TransferSize"))
                );
                ioTraceRecCount.increment();
            };
        }

        testContainerIoTraceLogRecords(stepId, ioTraceRecFunc);

        System.out.println("EX: " + expectedCount + "\nSUM: " + ioTraceRecCount.sum() + "\nDEL: " + expectedCount * requiredAccuracy);
        assertEquals(expectedCount, ioTraceRecCount.sum(), expectedCount * requiredAccuracy);

//        final List<CSVRecord> items = new ArrayList<>();
//        try (final BufferedReader br = new BufferedReader(new FileReader(hostItemOutputFile))) {
//            final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
//            for (final CSVRecord csvRecord : csvParser) {
//                items.add(csvRecord);
//            }
//        }
//
//        assertEquals(expectedCount, items.size(), expectedCount * requiredAccuracy);
//        final Frequency freq = new Frequency();
//        String itemPath, itemId;
//        long itemOffset;
//        long size;
//        String modLayerAndMask;
//        for (final CSVRecord itemRec : items) {
//            itemPath = itemRec.get(0);
//            itemId = itemPath.substring(itemPath.lastIndexOf('/') + 1);
//            itemOffset = Long.parseLong(itemRec.get(1), 0x10);
//            assertEquals(Long.parseLong(itemId, itemIdRadix), itemOffset);
//            freq.addValue(itemOffset);
//            size = Long.parseLong(itemRec.get(2));
//            assertEquals(itemSize.getValue().get(), size);
//            modLayerAndMask = itemRec.get(3);
//            assertEquals("0/0", modLayerAndMask);
//        }
//        assertEquals(items.size(), freq.getUniqueCount());
//
//        testTotalMetricsLogRecord(
//                getContainerMetricsTotalLogRecords(stepId).get(0), IoType.CREATE, concurrency.getValue(),
//                runMode.getNodeCount(), itemSize.getValue(), 0, 0
//        );
//
//        testMetricsLogRecords(
//                getContainerMetricsLogRecords(stepId), IoType.CREATE, concurrency.getValue(),
//                runMode.getNodeCount(), itemSize.getValue(), 0, 0,
//                averagePeriod
//        );
//
//        testSingleMetricsStdout(
//                stdOutContent.replaceAll("[\r\n]+", " "),
//                IoType.CREATE, concurrency.getValue(), runMode.getNodeCount(), itemSize.getValue(),
//                averagePeriod
//        );
//        testMetricsTableStdout(
//                stdOutContent, stepId, storageType, runMode.getNodeCount(), 0,
//                new HashMap<IoType, Integer>() {{
//                    put(IoType.CREATE, concurrency.getValue());
//                }}
//        );
//        testFinalMetricsTableRowStdout(
//                stdOutContent, stepId, IoType.CREATE, runMode.getNodeCount(), concurrency.getValue(),
//                0, 0, itemSize.getValue()
//        );
//
//        assertTrue(duration < timeoutInMillis);
    }
}
