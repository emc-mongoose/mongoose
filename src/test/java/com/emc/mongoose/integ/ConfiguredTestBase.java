package com.emc.mongoose.integ;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
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
	public static void before()
	throws Exception {
		RunTimeConfig.initContext();
		RUNTIME_CONFIG = RunTimeConfig.getContext();
	}
}
