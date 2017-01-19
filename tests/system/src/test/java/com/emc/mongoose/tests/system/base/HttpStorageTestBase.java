package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.impl.http.StorageMockFactory;
import com.emc.mongoose.ui.config.Config.StorageConfig;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 19.01.17.
 */
public abstract class HttpStorageTestBase
extends ConfiguredTestBase {

	private static StorageMockFactory STORAGE_MOCK_FACTORY;
	protected static StorageMock STORAGE_MOCK;
	protected static int HEAD_COUNT = 4;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ConfiguredTestBase.setUpClass();
		final StorageConfig storageConfig = CONFIG.getStorageConfig();
		CONFIG.getStorageConfig().getMockConfig().setHeadCount(HEAD_COUNT);
		final boolean sslFlag = CONFIG.getStorageConfig().getSsl();
		final List<String> nodeAddrs = new ArrayList<>();
		if(sslFlag) {
			for(int i = 0; i < HEAD_COUNT; i ++) {
				if(i % 2 == 1) {
					nodeAddrs.add("127.0.0.1:" + i);
				}
			}
		} else {
			for(int i = 0; i < HEAD_COUNT; i ++) {
				if(i % 2 == 0) {
					nodeAddrs.add("127.0.0.1:" + i);
				}
			}
		}
		storageConfig.getNodeConfig().setAddrs(nodeAddrs);
		STORAGE_MOCK_FACTORY = new StorageMockFactory(
			storageConfig, CONFIG.getLoadConfig(), CONFIG.getItemConfig()
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
