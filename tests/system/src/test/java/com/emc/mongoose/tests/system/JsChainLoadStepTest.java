package com.emc.mongoose.tests.system;

import com.emc.mongoose.api.common.env.PathUtil;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.DirWithManyFilesDeleter;
import static com.emc.mongoose.api.common.Constants.DIR_EXAMPLE_SCENARIO;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;

import com.github.akurilov.commons.system.SizeInBytes;

import com.github.dockerjava.core.command.WaitContainerResultCallback;

import org.apache.commons.csv.CSVRecord;

import org.junit.After;
import org.junit.Before;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 09.06.17.
 */
public class JsChainLoadStepTest
extends ScenarioTestBase {

	private static final long COUNT_LIMIT = 100_000;

	private String itemOutputPath;
	private String stdOutput;
	private long duration;
	private int containerExitCode;

	public JsChainLoadStepTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {
		configArgs.add("--test-step-limit-count=" + COUNT_LIMIT);
		super.setUp();
		switch(storageType) {
			case FS:
				itemOutputPath = Paths.get(
					Paths.get(PathUtil.getBaseDir()).getParent().toString(), stepId
				).toString();
				configArgs.add("--item-output-path=" + itemOutputPath);
				break;
			case SWIFT:
				configArgs.add("--storage-net-http-namespace=ns1");
				break;
		}
		initTestContainer();

		duration = System.currentTimeMillis();
		dockerClient.startContainerCmd(testContainerId).exec();
		containerExitCode = dockerClient
			.waitContainerCmd(testContainerId)
			.exec(new WaitContainerResultCallback())
			.awaitStatusCode(1000, TimeUnit.SECONDS);
		duration = System.currentTimeMillis() - duration;
		stdOutput = stdOutBuff.toString();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_EXAMPLE_SCENARIO, "js", "types", "chain.js");
	}

	public final void test()
	throws Exception {
		testMetricsTableStdout(
			stdOutput, stepId, driverCount.getValue(), COUNT_LIMIT,
			new HashMap<IoType, Integer>() {{
				put(IoType.CREATE, concurrency.getValue());
				put(IoType.READ, concurrency.getValue());
				put(IoType.DELETE, concurrency.getValue());
			}}
		);
		testFinalMetricsTableRowStdout(
			stdOutput, stepId, IoType.CREATE, driverCount.getValue(), concurrency.getValue(),
			COUNT_LIMIT, 0, itemSize.getValue()
		);
		testFinalMetricsTableRowStdout(
			stdOutput, stepId, IoType.READ, driverCount.getValue(), concurrency.getValue(),
			COUNT_LIMIT, 0, itemSize.getValue()
		);
		testFinalMetricsTableRowStdout(
			stdOutput, stepId, IoType.DELETE, driverCount.getValue(), concurrency.getValue(),
			COUNT_LIMIT, 0, new SizeInBytes(0)
		);
		final List<CSVRecord> totalRecs = getContainerMetricsTotalLogRecords();
		testTotalMetricsLogRecord(
			totalRecs.get(0), IoType.CREATE, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), COUNT_LIMIT, 0
		);
		testTotalMetricsLogRecord(
			totalRecs.get(1), IoType.READ, concurrency.getValue(), driverCount.getValue(),
			itemSize.getValue(), COUNT_LIMIT, 0
		);
		testTotalMetricsLogRecord(
			totalRecs.get(2), IoType.DELETE, concurrency.getValue(), driverCount.getValue(),
			new SizeInBytes(0), COUNT_LIMIT, 0
		);
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
}
