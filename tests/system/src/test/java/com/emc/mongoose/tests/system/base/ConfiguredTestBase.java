package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 19.01.17.
 */
public abstract class ConfiguredTestBase
extends LoggingTestBase {

	protected static Config CONFIG;
	protected static List<String> CONFIG_ARGS = new ArrayList<>();

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LoggingTestBase.setUpClass();
		CONFIG = Config.loadDefaults();
		if(CONFIG_ARGS != null) {
			CONFIG.apply(
				CliArgParser.parseArgs(
					CONFIG.getAliasingConfig(), CONFIG_ARGS.toArray(new String[CONFIG_ARGS.size()])
				),
				"systest-" + LogUtil.getDateTimeStamp()
			);
		}
		CONFIG.getTestConfig().getStepConfig().setId(STEP_ID);
		CONFIG.getTestConfig().getStepConfig().setIdTmp(false);
		CONFIG.getOutputConfig().getMetricsConfig().getTraceConfig().setPersist(true);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LoggingTestBase.tearDownClass();
		CONFIG_ARGS.clear();
	}
}
