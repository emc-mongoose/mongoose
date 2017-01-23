package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.impl.http.StorageMockFactory;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NodeConfig;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 19.01.17.
 */
public abstract class HttpStorageTestBase
extends ConfiguredTestBase {

	protected static List<StorageMock> STORAGE_MOCKS = new ArrayList<>();
	protected static int NODE_COUNT = 1;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ConfiguredTestBase.setUpClass();
		final StorageConfig storageConfig = CONFIG.getStorageConfig();
		final NodeConfig nodeConfig = storageConfig.getNodeConfig();
		final LoadConfig loadConfig = CONFIG.getLoadConfig();
		final ItemConfig itemConfig = CONFIG.getItemConfig();
		final int port = nodeConfig.getPort();
		final List<String> nodeAddrs = new ArrayList<>();
		StorageMock storageMock;
		for(int i = 0; i < NODE_COUNT; i ++) {
			nodeConfig.setPort(port + i);
			storageMock = new StorageMockFactory(storageConfig, loadConfig, itemConfig)
				.newStorageMock();
			storageMock.start();
			STORAGE_MOCKS.add(storageMock);
			nodeAddrs.add("127.0.0.1:" + port + i);
		}
		nodeConfig.setAddrs(nodeAddrs);
		nodeConfig.setPort(port);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		for(final StorageMock storageMock : STORAGE_MOCKS) {
			storageMock.close();
		}
		ConfiguredTestBase.tearDownClass();
	}
}
