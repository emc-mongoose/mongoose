package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.tests.system.util.EnvUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Created by andrey on 13.06.17.
 */
public class JsonReadFilesWithVariablePathTest
extends OldScenarioTestBase {

	private static final int EXPECTED_COUNT = 10000;

	private String fileOutputPath;
	private String stdOutput;

	public JsonReadFilesWithVariablePathTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {
		configArgs.add("--item-naming-radix=16");
		configArgs.add("--item-naming-length=16");
		super.setUp();
		fileOutputPath = Paths
			.get(Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId)
			.toString();
		try {
			DirWithManyFilesDeleter.deleteExternal(fileOutputPath);
		} catch(final Throwable ignored) {
		}
		EnvUtil.set("FILE_OUTPUT_PATH", fileOutputPath);
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		scenario.run();
		LogUtil.flushAll();
		stdOutput = stdOutStream.stopRecordingAndGet();
		TimeUnit.SECONDS.sleep(5);
	}

	@After
	public void tearDown()
	throws Exception {
		try {
			DirWithManyFilesDeleter.deleteExternal(fileOutputPath);
		} catch(final Exception e) {
			e.printStackTrace(System.err);
		}
		super.tearDown();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "ReadFilesWithVariablePath.json");
	}

	@Override
	protected String makeStepId() {
		return JsonReadFilesWithVariablePathTest.class.getSimpleName() + '-' +
			storageType.name() + '-' + driverCount.name() + 'x' + concurrency.name() + '-' +
			itemSize.name();
	}

	@Override
	public void test()
	throws Exception {

		final LongAdder ioTraceRecCount = new LongAdder();
		final int baseOutputPathLen = fileOutputPath.length();
		// Item path should look like:
		// ${FILE_OUTPUT_PATH}/1/b/0123456789abcdef
		// ${FILE_OUTPUT_PATH}/b/fedcba9876543210
		final Pattern subPathPtrn = Pattern.compile("(/[0-9a-f]){1,2}/[0-9a-f]{16}");
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			testIoTraceRecord(ioTraceRec, IoType.READ.ordinal(), itemSize.getValue());
			String nextFilePath = ioTraceRec.get("ItemPath");
			assertTrue(nextFilePath.startsWith(fileOutputPath));
			nextFilePath = nextFilePath.substring(baseOutputPathLen);
			final Matcher m = subPathPtrn.matcher(nextFilePath);
			assertTrue(m.matches());
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceReqTestFunc);
		assertEquals(
			"There should be more than 1 record in the I/O trace log file",
			EXPECTED_COUNT, ioTraceRecCount.sum()
		);

		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			EXPECTED_COUNT, 0, config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			EXPECTED_COUNT, 0
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
	}
}
