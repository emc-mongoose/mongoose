package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by kurila on 02.11.15.
 */
public class CambridgeLabViprTestBase
extends LoggingTestBase {
	//
	private static String STORAGE_ADDRS_DEFAULT;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LoggingTestBase.setUpClass();
		final AppConfig appConfig = BasicConfig.CONTEXT_CONFIG.get();
		STORAGE_ADDRS_DEFAULT = appConfig.getString(RunTimeConfig.KEY_STORAGE_ADDRS);
		appConfig.set(RunTimeConfig.KEY_STORAGE_ADDRS, "10.249.237.73,10.249.237.74,10.249.237.75");
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LoggingTestBase.tearDownClass();
		final AppConfig appConfig = BasicConfig.CONTEXT_CONFIG.get();
		appConfig.set(RunTimeConfig.KEY_STORAGE_ADDRS, STORAGE_ADDRS_DEFAULT); // reset to default
	}
}
