package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.load.builder.WSDataLoadBuilder;
import com.emc.mongoose.core.api.load.executor.WSDataLoadExecutor;
import com.emc.mongoose.core.impl.load.builder.BasicWSDataLoadBuilder;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class StandaloneLoadBuilderTestBase
extends WSMockTestBase {
	//
	protected static WSDataLoadBuilder<HttpDataItem, WSDataLoadExecutor<HttpDataItem>> LOAD_BUILDER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_STANDALONE);
		LOAD_BUILDER = new BasicWSDataLoadBuilder<>(RunTimeConfig.getContext())
			.setRunTimeConfig(rtConfig);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER.close();
		WSMockTestBase.tearDownClass();
	}
}
