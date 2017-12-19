package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.OldScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 12.06.17.
 */

public class JsonReadVerificationAfterCircularUpdateTest
extends OldScenarioTestBase {

	private String itemOutputPath;
	private String stdOutput;

	public JsonReadVerificationAfterCircularUpdateTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(
			getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "ReadVerificationAfterCircularUpdate.json"
		);
	}

	@Override
	protected String makeStepId() {
		return JsonReadVerificationAfterCircularUpdateTest.class.getSimpleName() + '-' +
			storageType.name() + '-' + driverCount.name() + 'x' + concurrency.name() + '-' +
			itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {
		configArgs.add("--storage-net-http-namespace=ns1");
		super.setUp();
		if(storageType.equals(StorageType.FS)) {
			itemOutputPath = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
			).toString();
			config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
		}
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
		if(storageType.equals(StorageType.FS)) {
			try {
				DirWithManyFilesDeleter.deleteExternal(itemOutputPath);
			} catch(final Exception e) {
				e.printStackTrace(System.err);
			}
		}
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {

		final LongAdder ioTraceRecCount = new LongAdder();
		final Consumer<CSVRecord> ioTraceReqTestFunc = ioTraceRec -> {
			assertEquals(
				"Record #" + ioTraceRecCount.sum() + ": unexpected operation type " +
					ioTraceRec.get("IoTypeCode"),
				IoType.READ, IoType.values()[Integer.parseInt(ioTraceRec.get("IoTypeCode"))]
			);
			assertEquals(
				"Record #" + ioTraceRecCount.sum() + ": unexpected status code " +
					ioTraceRec.get("StatusCode"),
				IoTask.Status.SUCC,
				IoTask.Status.values()[Integer.parseInt(ioTraceRec.get("StatusCode"))]
			);
		};
		testIoTraceLogRecords(ioTraceReqTestFunc);

		testTotalMetricsLogRecord(
			getMetricsTotalLogRecords().get(0),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), 0, 0
		);

		testMetricsLogRecords(
			getMetricsLogRecords(),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(), 0, 0,
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);

		testSingleMetricsStdout(
			stdOutput.replaceAll("[\r\n]+", " "),
			IoType.READ, concurrency.getValue(), driverCount.getValue(), itemSize.getValue(),
			config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod()
		);
		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), 0,
			new HashMap<IoType, Integer>() {{
				put(IoType.CREATE, concurrency.getValue());
				put(IoType.UPDATE, concurrency.getValue());
				put(IoType.READ, concurrency.getValue());
			}}
		);
	}
}
