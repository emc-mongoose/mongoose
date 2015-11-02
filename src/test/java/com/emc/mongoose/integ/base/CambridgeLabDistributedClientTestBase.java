package com.emc.mongoose.integ.base;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicStorageClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by kurila on 02.11.15.
 */
public class CambridgeLabDistributedClientTestBase
extends CambridgeLabDistributedTestBase {
	//
	protected static StorageClientBuilder<WSObject, StorageClient<WSObject>>
		CLIENT_BUILDER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		CambridgeLabDistributedTestBase.setUpClass();
		CLIENT_BUILDER = new BasicStorageClientBuilder<WSObject, StorageClient<WSObject>>()
			.setClientMode(LOAD_SVC_ADDRS_CUSTOM);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CLIENT_BUILDER.setClientMode(null);
		CambridgeLabDistributedTestBase.tearDownClass();
	}
}
