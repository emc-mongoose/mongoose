package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.impl.http.StorageMockFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 Created by andrey on 19.01.17.
 */
public abstract class HttpStorageTestBase
extends ConfiguredTestBase {

	private static StorageMockFactory STORAGE_MOCK_FACTORY;
	protected static StorageMock STORAGE_MOCK;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ConfiguredTestBase.setUpClass();
		STORAGE_MOCK_FACTORY = new StorageMockFactory(
			CONFIG.getStorageConfig(), CONFIG.getLoadConfig(), CONFIG.getItemConfig()
		);
		STORAGE_MOCK = STORAGE_MOCK_FACTORY.newStorageMock();
		STORAGE_MOCK.start();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		STORAGE_MOCK.close();
		ConfiguredTestBase.tearDownClass();
	}
}
