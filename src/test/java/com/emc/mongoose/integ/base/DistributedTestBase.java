package com.emc.mongoose.integ.base;
//
import static com.emc.mongoose.common.conf.Constants.*;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.integ.suite.WSMockTestSuite;
//
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
//
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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
		RT_CONFIG.set(RunTimeConfig.KEY_REMOTE_SERVE_IF_NOT_LOAD_SERVER, true);
		RT_CONFIG.set(RunTimeConfig.KEY_LOAD_SERVERS, ServiceUtils.getHostAddr());
		RT_CONFIG.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_SERVER);
		ServiceUtils.init();
		LOAD_BUILDER_SVC = new BasicWSLoadBuilderSvc<>(RT_CONFIG);
		LOAD_BUILDER_SVC.start();
		LOG.info(Markers.MSG, "Load builder service started");
		RT_CONFIG.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_CLIENT);
		RT_CONFIG.set(RunTimeConfig.KEY_REMOTE_PORT_EXPORT, 1299);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER_SVC.close();
		LOG.info(Markers.MSG, "Load builder service stopped");
		ServiceUtils.shutdown();
		WSMockTestBase.tearDownClass();
	}
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
	}
	//
	@After
	public void tearDown()
	throws Exception {
		super.tearDown();
	}
}
