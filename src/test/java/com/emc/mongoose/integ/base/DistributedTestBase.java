package com.emc.mongoose.integ.base;
//
import static com.emc.mongoose.common.conf.Constants.*;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.ServiceUtil;
//
//
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSDataLoadBuilderSvc;
//
import com.emc.mongoose.util.builder.LoadBuilderFactory;
import com.emc.mongoose.util.builder.SvcLoadBuildersRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.List;

/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedTestBase
extends WSMockTestBase {
	//
	protected static List<LoadBuilderSvc> LOAD_BUILDER_SERVICES;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_SERVER_ADDRS, ServiceUtil.getHostAddr());
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_SERVER);
		ServiceUtil.init();
		//
		LOAD_BUILDER_SERVICES = SvcLoadBuildersRunner.getSvcBuilders(rtConfig);
		SvcLoadBuildersRunner.startSvcLoadBuilders(LOAD_BUILDER_SERVICES);
		//
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_CLIENT);
		rtConfig.set(RunTimeConfig.KEY_REMOTE_SERVE_JMX, false);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		for(final LoadBuilder svc : LOAD_BUILDER_SERVICES) {
			svc.close();
		}
		RunTimeConfig.getContext().set(RunTimeConfig.KEY_RUN_MODE, RUN_MODE_STANDALONE);
		WSMockTestBase.tearDownClass();
	}
}
