package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.client.api.load.executor.WSDataLoadClient;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.util.builder.LoadBuilderFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedLoadBuilderTestBase
extends DistributedTestBase {
	//
	protected static LoadBuilder<WSObject, WSDataLoadClient<WSObject>> LOAD_BUILDER_CLIENT;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		DistributedTestBase.setUpClass();
		LOAD_BUILDER_CLIENT = LoadBuilderFactory.getInstance(RunTimeConfig.getContext());
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER_CLIENT.close();
		DistributedTestBase.tearDownClass();
	}
}
