package com.emc.mongoose.tests.system.base;

import com.emc.nagaina.impl.http.StorageMockFactory;

import com.emc.mongoose.api.model.concurrent.Daemon;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.storage.driver.service.BasicStorageDriverBuilderSvc;
import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.input.InputConfig;
import com.emc.mongoose.ui.config.item.naming.NamingConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.driver.DriverConfig;
import com.emc.mongoose.ui.config.storage.mock.MockConfig;
import com.emc.mongoose.ui.config.storage.mock.container.ContainerConfig;
import com.emc.mongoose.ui.config.storage.mock.fail.FailConfig;
import com.emc.mongoose.ui.config.storage.net.NetConfig;
import com.emc.mongoose.ui.config.storage.net.node.NodeConfig;
import com.emc.mongoose.ui.config.test.step.StepConfig;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 Created by andrey on 11.08.17.
 */
public abstract class StorageTestBase
extends ConfiguredTestBase {

	protected Map<String, Daemon> httpStorageMocks = null;
	protected int httpStorageNodeCount = 1;
	protected List<StorageDriverBuilderSvc> storageDriverBuilderSvcs = null;

	protected StorageTestBase(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		setUpHttpStorageMockIfNeeded();
		setUpDistributedModeIfNeeded();
	}

	@After
	public void tearDown()
	throws Exception {
		tearDownDistributedModeIfNeeded();
		tearDownHttpStorageMockIfNeeded();
		super.tearDown();
	}

	private void setUpHttpStorageMockIfNeeded()
	throws Exception {
		final StorageConfig storageConfig = config.getStorageConfig();
		storageConfig.getDriverConfig().setType(storageType.name().toLowerCase());
		switch(storageType) {
			case ATMOS:
			case AMZS3:
			case SWIFT:
				httpStorageMocks = new HashMap<>();
				final NodeConfig nodeConfig = storageConfig.getNetConfig().getNodeConfig();
				final ItemConfig itemConfig = config.getItemConfig();
				final StepConfig stepConfig = config.getTestConfig().getStepConfig();
				final int port = nodeConfig.getPort();
				final List<String> nodeAddrs = new ArrayList<>();
				String nextNodeAddr;
				Daemon storageMock;
				final MockConfig mockConfig = storageConfig.getMockConfig();
				final NetConfig netConfig = storageConfig.getNetConfig();
				final ContainerConfig containerConfig = mockConfig.getContainerConfig();
				final FailConfig failConfig = mockConfig.getFailConfig();
				final NamingConfig namingConfig = itemConfig.getNamingConfig();
				final InputConfig dataInputConfig = itemConfig.getDataConfig().getInputConfig();
				final DataInput dataInput = DataInput.getInstance(
					dataInputConfig.getFile(), dataInputConfig.getSeed(),
					dataInputConfig.getLayerConfig().getSize(),
					dataInputConfig.getLayerConfig().getCache()
				);
				StorageMockFactory storageMockFactory;
				for(int i = 0; i < httpStorageNodeCount; i ++) {
					nodeConfig.setPort(port + i);
					storageMockFactory = new StorageMockFactory(
						itemConfig.getInputConfig().getFile(), mockConfig.getCapacity(),
						containerConfig.getCapacity(), containerConfig.getCountLimit(),
						(int) config.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod(),
						failConfig.getConnections(), failConfig.getResponses(), dataInput,
						netConfig.getNodeConfig().getPort(), netConfig.getSsl(),
						(float) config.getLoadConfig().getLimitConfig().getRate(),
						namingConfig.getPrefix(), namingConfig.getRadix()
					);
					storageMock = storageMockFactory.newStorageMock();
					nextNodeAddr = "127.0.0.1:" + (port + i);
					storageMock.start();
					httpStorageMocks.put(nextNodeAddr, storageMock);
					nodeAddrs.add(nextNodeAddr);
				}
				nodeConfig.setAddrs(nodeAddrs);
				nodeConfig.setPort(port);
				break;
			case FS:
				break;
		}
	}

	private void tearDownHttpStorageMockIfNeeded()
	throws Exception {
		if(httpStorageMocks != null) {
			for(final Daemon storageMock : httpStorageMocks.values()) {
				storageMock.close();
			}
			httpStorageMocks.clear();
			httpStorageMocks = null;
		}
	}

	private void setUpDistributedModeIfNeeded()
	throws Exception {
		final int n = driverCount.getValue();
		if(n > 1) {
			storageDriverBuilderSvcs = new ArrayList<>(n);
			final DriverConfig driverConfig = config.getStorageConfig().getDriverConfig();
			final List<String> storageDriverAddrs = new ArrayList<>(n);
			int nextStorageDriverPort;
			StorageDriverBuilderSvc nextStorageDriverBuilder;
			for(int i = 0; i < n; i ++) {
				nextStorageDriverPort = driverConfig.getPort() + i;
				nextStorageDriverBuilder = new BasicStorageDriverBuilderSvc(nextStorageDriverPort);
				nextStorageDriverBuilder.start();
				storageDriverBuilderSvcs.add(nextStorageDriverBuilder);
				storageDriverAddrs.add("127.0.0.1:" + nextStorageDriverPort);
			}
			driverConfig.setAddrs(storageDriverAddrs);
			driverConfig.setRemote(true);
		}
	}

	private void tearDownDistributedModeIfNeeded()
	throws Exception {
		if(driverCount.equals(DriverCount.DISTRIBUTED) && storageDriverBuilderSvcs != null) {
			for(final StorageDriverBuilderSvc svc : storageDriverBuilderSvcs) {
				svc.close();
			}
			storageDriverBuilderSvcs.clear();
		}
	}
}
