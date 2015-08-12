package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.net.ServiceUtils;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class DistributedClientTestBase
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
	protected StorageClientBuilder<WSObject, StorageClient<WSObject>> clientBuilder;
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		clientBuilder = new BasicWSClientBuilder<>()
			.setClientMode(new String[] {ServiceUtils.getHostAddr()});
	}
	//
	@After
	public void tearDown()
	throws Exception {
		super.tearDown();
	}
}
