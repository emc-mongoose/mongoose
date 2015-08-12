package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class StandaloneClientTestBase
extends WSMockTestBase {
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestBase.setUpClass();
	}
	//
	protected StorageClientBuilder clientBuilder;
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		clientBuilder = new BasicWSClientBuilder();
	}
	//
	@After
	public void tearDown()
	throws Exception {
		super.tearDown();
	}
}
