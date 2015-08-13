package com.emc.mongoose.integ.base;
//
import static com.emc.mongoose.common.conf.Constants.*;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
//
//
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedTestBase
extends WSMockTestBase {
	//
	protected static LoadBuilderSvc<WSObject, WSLoadSvc<WSObject>> LOAD_BUILDER_SVC;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_REMOTE_SERVE_IF_NOT_LOAD_SERVER, true);
		rtConfig.set(RunTimeConfig.KEY_LOAD_SERVERS, ServiceUtils.getHostAddr());
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_SERVER);
		ServiceUtils.init();
		LOAD_BUILDER_SVC = new BasicWSLoadBuilderSvc<>(rtConfig);
		LOAD_BUILDER_SVC.start();
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_CLIENT);
		rtConfig.set(RunTimeConfig.KEY_REMOTE_SERVE_IF_NOT_LOAD_SERVER, false);
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
