package com.emc.mongoose.system.base;
//
import static com.emc.mongoose.common.conf.Constants.*;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
//
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedTestBase
extends HttpStorageMockTestBase {
	//
	protected static LoadBuilderSvc LOAD_BUILDER_SVC;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		HttpStorageMockTestBase.setUpClass();
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.setProperty(AppConfig.KEY_LOAD_SERVER_ADDRS, ServiceUtil.getHostAddr());
		appConfig.setProperty(AppConfig.KEY_RUN_MODE, RUN_MODE_SERVER);
		ServiceUtil.init();
		LOAD_BUILDER_SVC = new MultiLoadBuilderSvc(appConfig);
		LOAD_BUILDER_SVC.start();
		appConfig.setProperty(AppConfig.KEY_RUN_MODE, RUN_MODE_CLIENT);
		appConfig.setProperty(AppConfig.KEY_NETWORK_SERVE_JMX, false);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER_SVC.close();
		BasicConfig.THREAD_CONTEXT.get().setProperty(AppConfig.KEY_RUN_MODE, RUN_MODE_STANDALONE);
		HttpStorageMockTestBase.tearDownClass();
	}
}
