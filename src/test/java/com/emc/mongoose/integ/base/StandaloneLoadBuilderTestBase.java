package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
//
import com.emc.mongoose.util.factory.LoadBuilderFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class StandaloneLoadBuilderTestBase
extends WSMockTestBase {
	//
	protected static LoadBuilder<WSObject, WSLoadExecutor<WSObject>> LOAD_BUILDER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_STANDALONE);
		LOAD_BUILDER = LoadBuilderFactory
			.<WSObject, WSLoadExecutor<WSObject>>getInstance(RunTimeConfig.getContext())
			.setProperties(rtConfig);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER.close();
		WSMockTestBase.tearDownClass();
	}
}
