package com.emc.mongoose.integ.base;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static com.emc.mongoose.common.conf.Constants.RUN_MODE_CLIENT;
import static com.emc.mongoose.common.conf.Constants.RUN_MODE_SERVER;
import static com.emc.mongoose.common.conf.Constants.RUN_MODE_STANDALONE;
/**
 Created by kurila on 05.12.15.
 */
public class DistributedFileSystemTestBase
extends FileSystemTestBase {
	//
	protected static LoadBuilderSvc LOAD_BUILDER_SVC;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		FileSystemTestBase.setUpClass();
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		appConfig.set(RunTimeConfig.KEY_LOAD_SERVER_ADDRS, ServiceUtil.getHostAddr());
		appConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_SERVER);
		ServiceUtil.init();
		LOAD_BUILDER_SVC = new MultiLoadBuilderSvc(appConfig);
		LOAD_BUILDER_SVC.start();
		appConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_CLIENT);
		appConfig.set(RunTimeConfig.KEY_REMOTE_SERVE_JMX, false);
		CLIENT_BUILDER.setClientMode(new String[] {ServiceUtil.getHostAddr()});
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CLIENT_BUILDER.setClientMode(null);
		LOAD_BUILDER_SVC.close();
		BasicConfig.THREAD_CONTEXT.get().set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_STANDALONE);
		FileSystemTestBase.tearDownClass();
	}
}
