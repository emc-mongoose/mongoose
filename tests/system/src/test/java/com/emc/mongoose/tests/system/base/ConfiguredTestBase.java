package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;

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
		CONFIG = ConfigParser.loadDefaultConfig();
		if(CONFIG_ARGS != null) {
			CONFIG.apply(
				CliArgParser.parseArgs(
					CONFIG.getAliasingConfig(), CONFIG_ARGS.toArray(new String[CONFIG_ARGS.size()])
				)
			);
		}
		CONFIG.getTestConfig().getStepConfig().setId(STEP_NAME);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LoggingTestBase.tearDownClass();
		CONFIG_ARGS.clear();
	}
}
