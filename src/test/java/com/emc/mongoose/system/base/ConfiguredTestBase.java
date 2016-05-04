package com.emc.mongoose.system.base;
//
import static com.emc.mongoose.common.conf.AppConfig.*;
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
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(KEY_ITEM_TYPE, "data");
	}
}
