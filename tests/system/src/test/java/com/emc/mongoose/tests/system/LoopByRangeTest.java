package com.emc.mongoose.tests.system;

import com.emc.mongoose.run.scenario.JsonScenario;
import com.emc.mongoose.tests.system.base.ScenarioTestBase;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.tests.system.util.LogPatterns;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import static com.emc.mongoose.run.scenario.Scenario.DIR_SCENARIO;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Created by andrey on 08.06.17.
 */
public final class LoopByRangeTest
extends ScenarioTestBase {

	private static final double EXPECTED_LOOP_START_VALUE = 2.71828182846;
	private static final double EXPECTED_LOOP_LIMIT_VALUE = 3.1415926;
	private static final double EXPECTED_STEP_VALUE = 0.1;
	private static final Pattern PTRN_LOOP_STEP_MSG = Pattern.compile(
		LogPatterns.ASCII_COLOR.pattern() + LogPatterns.DATE_TIME_ISO8601.pattern() +
			"\\s+" + LogPatterns.STD_OUT_LOG_LEVEL.pattern() +
			"\\s+" + LogPatterns.STD_OUT_CLASS_NAME.pattern() +
			"\\s+" + LogPatterns.STD_OUT_THREAD_NAME.pattern() +
			"\\s+Use\\snext\\svalue\\sfor\\s\"i\":\\s(?<stepValue>\\d\\.\\d+)"
	);

	private String stdOutput;

	public LoopByRangeTest(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Override
	protected String makeStepId() {
		return LoopByCountTest.class.getSimpleName() + '-' + storageType.name() + '-' +
			driverCount.name() + 'x' + concurrency.name() + '-' + itemSize.name();
	}

	@Override
	protected Path makeScenarioPath() {
		return Paths.get(getBaseDir(), DIR_SCENARIO, "systest", "LoopByRange.json");
	}

	@Before
	public final void setUp()
	throws Exception {
		super.setUp();
		scenario = new JsonScenario(config, scenarioPath.toFile());
		stdOutStream.startRecording();
		scenario.run();
		LogUtil.flushAll();
		stdOutput = stdOutStream.stopRecordingAndGet();
	}

	@After
	public final void tearDown()
	throws Exception {
		super.tearDown();
	}

	@Override
	public void test()
	throws Exception {
		final Matcher m = PTRN_LOOP_STEP_MSG.matcher(stdOutput);
		double nextExpectedStepVal = EXPECTED_LOOP_START_VALUE;
		while(m.find()) {
			final String t = m.group("stepValue");
			assertEquals(nextExpectedStepVal, Double.parseDouble(t), EXPECTED_STEP_VALUE / 100);
			assertTrue(nextExpectedStepVal <= EXPECTED_LOOP_LIMIT_VALUE);
			nextExpectedStepVal += EXPECTED_STEP_VALUE;
		}
	}
}
