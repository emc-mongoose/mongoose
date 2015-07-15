package com.emc.mongoose.integ.suite;
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
import org.apache.logging.log4j.LogManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 Created by kurila on 15.07.15.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	com.emc.mongoose.integ.distributed.single.WriteByCountTest.class
})
public class DistributedLoadTestSuite
extends WSMockTestSuite {
	//
	protected static LoadBuilderSvc<WSObject, WSLoadSvc<WSObject>> LOAD_BUILDER_SVC;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestSuite.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_REMOTE_SERVE_IF_NOT_LOAD_SERVER, true);
		ServiceUtils.init();
		LOAD_BUILDER_SVC = new BasicWSLoadBuilderSvc<>(rtConfig);
		LOAD_BUILDER_SVC.start();
		LogManager.getLogger().info(Markers.MSG, "Load builder service started");
		rtConfig.set(RunTimeConfig.KEY_REMOTE_PORT_EXPORT, 1299);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER_SVC.close();
		LogManager.getLogger().info(Markers.MSG, "Load builder service stopped");
		ServiceUtils.shutdown();
		WSMockTestSuite.tearDownClass();
	}
}
