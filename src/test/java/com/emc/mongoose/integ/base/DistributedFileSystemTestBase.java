package com.emc.mongoose.integ.base;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.util.builder.LoadBuilderFactory;
import com.emc.mongoose.util.builder.MultiLoadBuilderSvc;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicStorageClientBuilder;
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
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_SERVER_ADDRS, ServiceUtil.getHostAddr());
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_SERVER);
		ServiceUtil.init();
		LOAD_BUILDER_SVC = new MultiLoadBuilderSvc(rtConfig);
		LOAD_BUILDER_SVC.start();
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_CLIENT);
		rtConfig.set(RunTimeConfig.KEY_REMOTE_SERVE_JMX, false);
		CLIENT_BUILDER.setClientMode(new String[] {ServiceUtil.getHostAddr()});
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CLIENT_BUILDER.setClientMode(null);
		LOAD_BUILDER_SVC.close();
		RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_STANDALONE);
		FileSystemTestBase.tearDownClass();
	}
}
