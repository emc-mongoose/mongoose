package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.client.api.load.builder.WSLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.WSLoadClient;
import com.emc.mongoose.client.impl.load.builder.BasicWSLoadBuilderClient;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.WSObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedLoadBuilderTestBase
extends DistributedTestBase {
	//
	protected static WSLoadBuilderClient<WSObject, WSLoadClient<WSObject>> LOAD_BUILDER_CLIENT;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		DistributedTestBase.setUpClass();
		LOAD_BUILDER_CLIENT = new BasicWSLoadBuilderClient<>(RunTimeConfig.getContext());
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER_CLIENT.close();
		DistributedTestBase.tearDownClass();
	}
}
