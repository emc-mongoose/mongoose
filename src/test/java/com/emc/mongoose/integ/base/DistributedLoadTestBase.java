package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
//
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by kurila on 15.07.15.
 */
public class DistributedLoadTestBase
extends WSMockTestBase {
	//
	protected static LoadBuilderSvc<WSObject, WSLoadSvc<WSObject>> LOAD_BUILDER_SVC;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		RUNTIME_CONFIG.set(RunTimeConfig.KEY_REMOTE_SERVE_IF_NOT_LOAD_SERVER, true);
		ServiceUtils.init();
		LOAD_BUILDER_SVC = new BasicWSLoadBuilderSvc<>(RUNTIME_CONFIG);
		LOAD_BUILDER_SVC.start();
		LOG.info(Markers.MSG, "Load builder service started");
		RUNTIME_CONFIG.set(RunTimeConfig.KEY_REMOTE_PORT_EXPORT, 1299);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER_SVC.close();
		LOG.info(Markers.MSG, "Load builder service stopped");
		ServiceUtils.shutdown();
	}
}
