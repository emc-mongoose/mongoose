package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.config.Config;
import com.emc.mongoose.logging.LogUtil;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 19.01.17.
 */
public abstract class ConfiguredTestBase
extends LoggingTestBase {

	protected Config config;
	protected final List<String> configArgs = new ArrayList<>();

	protected ConfiguredTestBase(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		config = Config.loadDefaults();
		config.apply(
			CliArgParser.parseArgs(
				config.getAliasingConfig(), configArgs.toArray(new String[configArgs.size()])
			),
			"systest-" + LogUtil.getDateTimeStamp()
		);
		config.getTestConfig().getStepConfig().setId(stepId);
		config.getTestConfig().getStepConfig().setIdTmp(false);
		config.getOutputConfig().getMetricsConfig().getTraceConfig().setPersist(true);
		config.getItemConfig().getDataConfig().setSize(itemSize.getValue());
		config.getLoadConfig().getLimitConfig().setConcurrency(concurrency.getValue());
	}

	@After
	public void tearDown()
	throws Exception {
		configArgs.clear();
		super.tearDown();
	}
}
