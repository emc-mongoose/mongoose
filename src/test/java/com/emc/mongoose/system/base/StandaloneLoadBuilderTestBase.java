package com.emc.mongoose.system.base;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.builder.HttpDataLoadBuilder;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
import com.emc.mongoose.core.impl.load.builder.BasicHttpDataLoadBuilder;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class StandaloneLoadBuilderTestBase
extends WSMockTestBase {
	//
	protected static HttpDataLoadBuilder<HttpDataItem, HttpDataLoadExecutor<HttpDataItem>> LOAD_BUILDER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.setProperty(AppConfig.KEY_RUN_MODE, Constants.RUN_MODE_STANDALONE);
		LOAD_BUILDER = new BasicHttpDataLoadBuilder<>(appConfig);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER.close();
		WSMockTestBase.tearDownClass();
	}
}
