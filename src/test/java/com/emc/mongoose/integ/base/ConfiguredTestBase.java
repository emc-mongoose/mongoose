package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import org.junit.BeforeClass;
/**
 Created by kurila on 14.07.15.
 */
public abstract class ConfiguredTestBase
extends LoggingTestBase {
	//
	protected static RunTimeConfig RUNTIME_CONFIG;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		RunTimeConfig.initContext();
		RUNTIME_CONFIG = RunTimeConfig.getContext();
		LOG.info(Markers.MSG, "Shared runtime configuration has been initialized");
	}
}
