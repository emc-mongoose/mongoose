package com.emc.mongoose.integ.base;
//
import static com.emc.mongoose.common.conf.Constants.*;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
//
import com.emc.mongoose.util.builder.LoadBuilderFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedTestBase
extends WSMockTestBase {
	//
	protected static LoadBuilderSvc LOAD_BUILDER_SVC;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_SERVER_ADDRS, ServiceUtil.getHostAddr());
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_SERVER);
		ServiceUtil.init();
		LOAD_BUILDER_SVC = (LoadBuilderSvc) LoadBuilderFactory.getInstance(rtConfig);
		LOAD_BUILDER_SVC.start();
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_CLIENT);
		rtConfig.set(RunTimeConfig.KEY_REMOTE_SERVE_JMX, false);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER_SVC.close();
		RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_STANDALONE);
		WSMockTestBase.tearDownClass();
	}
}
