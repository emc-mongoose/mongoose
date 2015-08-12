package com.emc.mongoose.integ.base;
import com.emc.mongoose.client.api.load.builder.WSLoadBuilderClient;
import com.emc.mongoose.client.impl.load.builder.BasicWSLoadBuilderClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedLoadBuilderTestBase
extends DistributedTestBase {
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		DistributedTestBase.setUpClass();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		DistributedTestBase.setUpClass();
	}
	//
	protected WSLoadBuilderClient loadBuilderClient;
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		loadBuilderClient = new BasicWSLoadBuilderClient<>(RT_CONFIG);
	}
	//
	@After
	public void tearDown()
	throws Exception {
		loadBuilderClient.close();
		super.tearDown();
	}
}
