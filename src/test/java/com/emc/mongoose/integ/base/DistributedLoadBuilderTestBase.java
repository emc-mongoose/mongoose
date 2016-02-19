package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.client.api.load.builder.LoadBuilderClient;
//
//
//
//
import com.emc.mongoose.util.builder.LoadBuilderFactory;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedLoadBuilderTestBase
extends DistributedTestBase {
	//
	protected static LoadBuilderClient LOAD_BUILDER_CLIENT;
	//
	@BeforeClass @SuppressWarnings("unchecked")
	public static void setUpClass()
	throws Exception {
		DistributedTestBase.setUpClass();
		LOAD_BUILDER_CLIENT = (LoadBuilderClient) LoadBuilderFactory
			.getInstance(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LOAD_BUILDER_CLIENT.close();
		DistributedTestBase.tearDownClass();
	}
}
