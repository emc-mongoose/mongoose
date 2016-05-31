package com.emc.mongoose.system.base;
//
import static com.emc.mongoose.common.conf.AppConfig.*;
//
import com.emc.mongoose.common.conf.BasicConfig;
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
		BasicConfig.THREAD_CONTEXT.set(new BasicConfig());
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(KEY_ITEM_DATA_SIZE, "1MB");
		System.setProperty(KEY_ITEM_TYPE, "data");
		System.setProperty(KEY_LOAD_CIRCULAR, "false");
		System.setProperty(KEY_LOAD_LIMIT_COUNT, "0");
		System.setProperty(KEY_STORAGE_HTTP_API, "s3");
		System.setProperty(KEY_STORAGE_TYPE, "http");
	}
}
