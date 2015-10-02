package com.emc.mongoose.integ.base;
//
import static com.emc.mongoose.common.conf.RunTimeConfig.*;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class ConfiguredTestBase {
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		initContext();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
	}
}
