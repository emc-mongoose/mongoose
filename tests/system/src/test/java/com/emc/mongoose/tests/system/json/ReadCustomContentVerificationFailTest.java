package com.emc.mongoose.tests.system.json;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.scenario.json.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
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
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 Created by andrey on 12.06.17.
 */
public class ReadCustomContentVerificationFailTest
extends ScenarioTestBase {

	private String itemOutputPath;

	public ReadCustomContentVerificationFailTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "json", "systest", "ReadVerificationFail.json");
	}

	@Override
	protected String makeStepId() {
		return ReadCustomContentVerificationFailTest.class.getSimpleName() + '-' +
			storageType.name() + '-' + driverCount.name() + 'x' + concurrency.name() + '-' +
			itemSize.name();
	}

	@Before
	public void setUp()
	throws Exception {
		configArgs.add(
			"--item-data-input-file=" + PathUtil.getBaseDir() + "/example/content/textexample"
		);
		configArgs.add("--storage-net-http-namespace=ns1");
		super.setUp();
		if(StorageType.FS.equals(storageType)) {
			itemOutputPath = Paths.get(
				Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
			).toString();
			config.getItemConfig().getOutputConfig().setPath(itemOutputPath);
		}
		scenario = new JsonScenario(config, scenarioPath.toFile());
		scenario.run();
		LogUtil.flushAll();
		TimeUnit.SECONDS.sleep(5);
	}

	@After
	public void tearDown()
	throws Exception {
		if(StorageType.FS.equals(storageType)) {
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
		final Consumer<CSVRecord> ioTraceRecTestFunc = ioTraceRec -> {
			assertEquals(
				"Record #" + ioTraceRecCount.sum() + ": unexpected operation type " +
					ioTraceRec.get("IoTypeCode"),
				IoType.READ,
				IoType.values()[Integer.parseInt(ioTraceRec.get("IoTypeCode"))]
			);
			assertEquals(
				"Record #" + ioTraceRecCount.sum() + ": unexpected status code " +
					ioTraceRec.get("StatusCode"),
				IoTask.Status.RESP_FAIL_CORRUPT,
				IoTask.Status.values()[Integer.parseInt(ioTraceRec.get("StatusCode"))]
			);
			ioTraceRecCount.increment();
		};
		testIoTraceLogRecords(ioTraceRecTestFunc);
		assertTrue("The count of the I/O trace records should be > 0", ioTraceRecCount.sum() > 0);
	}
}
