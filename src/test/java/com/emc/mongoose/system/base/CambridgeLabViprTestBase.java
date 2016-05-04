package com.emc.mongoose.system.base;
//
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
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
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		STORAGE_ADDRS_DEFAULT = appConfig.getString(AppConfig.KEY_STORAGE_ADDRS);
		appConfig.setProperty(AppConfig.KEY_STORAGE_ADDRS, "10.249.237.73,10.249.237.74,10.249.237.75");
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LoggingTestBase.tearDownClass();
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.setProperty(AppConfig.KEY_STORAGE_ADDRS, STORAGE_ADDRS_DEFAULT); // reset to default
	}
}
