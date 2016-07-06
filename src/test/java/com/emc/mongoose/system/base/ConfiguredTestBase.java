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
		System.clearProperty(KEY_AUTH_ID);
		System.clearProperty(KEY_AUTH_SECRET);
		System.clearProperty(KEY_AUTH_TOKEN);
		System.clearProperty(KEY_ITEM_DATA_CONTENT_FILE);
		System.clearProperty(KEY_ITEM_DATA_RANGES);
		System.clearProperty(KEY_ITEM_DATA_SIZE);
		System.clearProperty(KEY_ITEM_DATA_VERIFY);
		System.clearProperty(KEY_ITEM_DST_CONTAINER);
		System.clearProperty(KEY_ITEM_DST_FILE);
		System.clearProperty(KEY_ITEM_NAMING_LENGTH);
		System.clearProperty(KEY_ITEM_NAMING_OFFSET);
		System.clearProperty(KEY_ITEM_NAMING_PREFIX);
		System.clearProperty(KEY_ITEM_NAMING_RADIX);
		System.clearProperty(KEY_ITEM_NAMING_TYPE);
		System.clearProperty(KEY_ITEM_SRC_CONTAINER);
		System.clearProperty(KEY_ITEM_SRC_FILE);
		System.clearProperty(KEY_ITEM_TYPE);
		System.clearProperty(KEY_LOAD_CIRCULAR);
		System.clearProperty(KEY_LOAD_LIMIT_COUNT);
		System.clearProperty(KEY_LOAD_LIMIT_RATE);
		System.clearProperty(KEY_LOAD_LIMIT_SIZE);
		System.clearProperty(KEY_LOAD_LIMIT_TIME);
		System.clearProperty(KEY_RUN_ID);
		System.clearProperty(KEY_STORAGE_HTTP_API);
		System.clearProperty(KEY_STORAGE_TYPE);
	}
}
