package com.emc.mongoose.tests.system.base.deprecated;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.storage.mock.impl.http.StorageMockFactory;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NetConfig.NodeConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;

import static org.junit.Assert.assertEquals;

import com.emc.mongoose.tests.system.base.ConfiguredTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 Created by andrey on 19.01.17.
 */
@Deprecated
public abstract class HttpStorageTestBase
extends ConfiguredTestBase {

	protected static Map<String, Daemon> STORAGE_MOCKS = new HashMap<>();
	protected static final int STORAGE_NODE_COUNT = 1;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ConfiguredTestBase.setUpClass();
		final StorageConfig storageConfig = CONFIG.getStorageConfig();
		final NodeConfig nodeConfig = storageConfig.getNetConfig().getNodeConfig();
		final ItemConfig itemConfig = CONFIG.getItemConfig();
		final StepConfig stepConfig = CONFIG.getTestConfig().getStepConfig();
		final int port = nodeConfig.getPort();
		final List<String> nodeAddrs = new ArrayList<>();
		String nextNodeAddr;
		Daemon storageMock;
		final StorageMockFactory storageMockFactory = new StorageMockFactory(
			storageConfig, itemConfig, stepConfig
		);
		for(int i = 0; i < STORAGE_NODE_COUNT; i ++) {
			nodeConfig.setPort(port + i);
			storageMock = storageMockFactory.newStorageMock();
			nextNodeAddr = "127.0.0.1:" + (port + i);
			storageMock.start();
			STORAGE_MOCKS.put(nextNodeAddr, storageMock);
			nodeAddrs.add(nextNodeAddr);
		}
		nodeConfig.setAddrs(nodeAddrs);
		nodeConfig.setPort(port);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		for(final Daemon storageMock : STORAGE_MOCKS.values()) {
			storageMock.close();
		}
		STORAGE_MOCKS.clear();
		ConfiguredTestBase.tearDownClass();
	}

	protected static void testHttpStorageMockContains(
		final String nodeAddr, final String itemPath, final long expectedSize
	) throws MalformedURLException, IOException {
		final URL itemUrl = new URL("http://" + nodeAddr + itemPath);
		long size = 0;
		int n;
		final byte buff[] = new byte[0x1000];
		try(final InputStream in = itemUrl.openStream()) {
			while(- 1 != (n = in.read(buff))) {
				size += n;
			}
		}
		assertEquals(expectedSize, size);
	}
}
