package com.emc.mongoose.system.base;
//
import com.emc.mongoose.core.api.v1.item.data.FileItem;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicStorageClientBuilder;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class FileSystemTestBase
extends LoggingTestBase {
	//
	protected static StorageClientBuilder<FileItem, StorageClient<FileItem>>
		CLIENT_BUILDER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LoggingTestBase.setUpClass();
		CLIENT_BUILDER = new BasicStorageClientBuilder<FileItem, StorageClient<FileItem>>()
			.setClientMode(null);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LoggingTestBase.tearDownClass();
	}
}
