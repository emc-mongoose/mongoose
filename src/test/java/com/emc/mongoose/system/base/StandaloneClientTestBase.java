package com.emc.mongoose.system.base;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicStorageClientBuilder;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class StandaloneClientTestBase
extends WSMockTestBase {
	//
	protected static StorageClientBuilder<HttpDataItem, StorageClient<HttpDataItem>>
		CLIENT_BUILDER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
		CLIENT_BUILDER = new BasicStorageClientBuilder<HttpDataItem, StorageClient<HttpDataItem>>()
			.setClientMode(null);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestBase.tearDownClass();
	}
}
