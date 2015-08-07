package com.emc.mongoose.integ.suite;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.LogManager;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 Created by kurila on 14.07.15.
 */
@RunWith(Suite.class)
public abstract class ConfiguredTestSuite
extends LoggingTestSuite {
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LoggingTestSuite.setUpClass();
		RunTimeConfig.initContext();
		RunTimeConfig.resetContext();
		LogManager.getLogger().info(Markers.MSG, "Shared runtime configuration has been initialized");
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		// place code here
		LoggingTestSuite.tearDownClass();
	}
}
