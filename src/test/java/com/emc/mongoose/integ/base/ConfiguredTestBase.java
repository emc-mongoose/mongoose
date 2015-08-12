package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class ConfiguredTestBase {
	//
	protected static RunTimeConfig RT_CONFIG;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		RunTimeConfig.initContext();
		RunTimeConfig.resetContext();
		RT_CONFIG = RunTimeConfig.getContext();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
	}
	//
	@Before
	public void setUp()
	throws Exception {
	}
	//
	@After
	public void tearDown()
	throws Exception {
	}
}
