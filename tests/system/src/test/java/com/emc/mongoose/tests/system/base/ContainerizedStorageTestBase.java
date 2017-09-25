package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.tests.system.base.params.Concurrency;
import com.emc.mongoose.tests.system.base.params.DriverCount;
import com.emc.mongoose.tests.system.base.params.ItemSize;
import com.emc.mongoose.tests.system.base.params.StorageType;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.naming.NamingConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.driver.DriverConfig;
import com.emc.mongoose.ui.config.storage.mock.MockConfig;
import com.emc.mongoose.ui.config.storage.mock.container.ContainerConfig;
import com.emc.mongoose.ui.config.storage.mock.fail.FailConfig;
import com.emc.mongoose.ui.config.storage.net.NetConfig;
import com.emc.mongoose.ui.config.storage.net.node.NodeConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;

import org.apache.logging.log4j.Level;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 Created by andrey on 23.09.17.
 */
public abstract class ContainerizedStorageTestBase
extends ConfiguredTestBase {

	protected static final String TEST_VERSION = System.getenv("TEST_VERSION");
	private static final String STORAGE_MOCK_IMAGE_NAME = "emcmongoose/nagaina";
	private static final String
		STORAGE_DRIVER_IMAGE_NAME = "emcmongoose/mongoose-storage-driver-service:" + TEST_VERSION;

	protected Map<String, String> httpStorageMocks = null;
	protected int httpStorageNodeCount = 1;
	protected List<String> storageDriverBuilderSvcs = null;
	protected DockerClient dockerClient = null;

	protected ContainerizedStorageTestBase(
		final StorageType storageType, final DriverCount driverCount, final Concurrency concurrency,
		final ItemSize itemSize
	) throws Exception {
		super(storageType, driverCount, concurrency, itemSize);
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		configArgs.add("--test-step-id=" + stepId);
		configArgs.add("--output-metrics-trace-persist=true");
		configArgs.add("--item-data-size=" + itemSize.getValue());
		configArgs.add("--load-limit-concurrency=" + concurrency.getValue());
		dockerClient = DockerClientBuilder.getInstance().build();
		setUpHttpStorageMockIfNeeded();
		setUpDistributedModeIfNeeded();
	}

	@After
	public void tearDown()
	throws Exception {
		tearDownDistributedModeIfNeeded();
		tearDownHttpStorageMockIfNeeded();
		dockerClient.close();
		super.tearDown();
	}

	private void setUpHttpStorageMockIfNeeded()
	throws Exception {
		final StorageConfig storageConfig = config.getStorageConfig();
		storageConfig.getDriverConfig().setType(storageType.name().toLowerCase());
		switch(storageType) {
			case ATMOS:
			case S3:
			case EMCS3:
			case SWIFT:
				Loggers.TEST.info("Image {} pulled", STORAGE_MOCK_IMAGE_NAME);
				httpStorageMocks = new HashMap<>();
				final NodeConfig nodeConfig = storageConfig.getNetConfig().getNodeConfig();
				final ItemConfig itemConfig = config.getItemConfig();
				final int port = nodeConfig.getPort();
				final List<String> nodeAddrs = new ArrayList<>();
				String nextNodeAddr;
				final MockConfig mockConfig = storageConfig.getMockConfig();
				final NetConfig netConfig = storageConfig.getNetConfig();
				final ContainerConfig
					containerConfig = mockConfig.getContainerConfig();
				final FailConfig failConfig = mockConfig.getFailConfig();
				final NamingConfig namingConfig = itemConfig.getNamingConfig();
				for(int i = 0; i < httpStorageNodeCount; i ++) {

					nodeConfig.setPort(port + i);
					nextNodeAddr = "127.0.0.1:" + (port + i);

					final List<String> cmd = new ArrayList<>();
					cmd.add("-Xms1g");
					cmd.add("-Xmx1g");
					cmd.add("-XX:MaxDirectMemorySize=1g");
					cmd.add("-jar");
					cmd.add("/opt/nagaina/nagaina.jar");
					if(itemConfig.getInputConfig().getFile() != null) {
						cmd.add("--item-input-file=" + itemConfig.getInputConfig().getFile());
					}
					if(namingConfig.getPrefix() != null) {
						cmd.add("--item-naming-prefix=" + namingConfig.getPrefix());
					}
					cmd.add("--item-naming-radix=" + namingConfig.getRadix());
					cmd.add("--storage-mock-capacity=" + mockConfig.getCapacity());
					cmd.add("--storage-mock-container-capacity=" + containerConfig.getCapacity());
					cmd.add("--storage-mock-container-countLimit=" + containerConfig.getCountLimit());
					cmd.add("--storage-mock-fail-connections=" + failConfig.getConnections());
					cmd.add("--storage-mock-fail-responses=" + failConfig.getResponses());
					cmd.add("--storage-net-node-port=" + nodeConfig.getPort());
					cmd.add("--storage-net-ssl=" + netConfig.getSsl());
					final double rateLimit = config.getLoadConfig().getLimitConfig().getRate();
					final long metricsPeriod = config
						.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod();
					cmd.add("--test-step-limit-rate=" + rateLimit);
					cmd.add("--test-step-metrics-period=" + metricsPeriod);

					final CreateContainerResponse container = dockerClient
						.createContainerCmd(STORAGE_MOCK_IMAGE_NAME)
						.withName("mongoose_storage_mock_" + (port + i))
						.withNetworkMode("host")
						.withExposedPorts(ExposedPort.tcp(port + i))
						.withEntrypoint("java")
						.withCmd(cmd)
						.exec();

					final String containedId = container.getId();
					dockerClient.startContainerCmd(containedId).exec();

					httpStorageMocks.put(nextNodeAddr, containedId);
					Loggers.TEST.info(
						"Started the storage mock service @ port #{} in the container {}",
						netConfig.getNodeConfig().getPort(), containedId
					);
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
			for(final String storageMockContainerId : httpStorageMocks.values()) {
				dockerClient.killContainerCmd(storageMockContainerId).exec();
				dockerClient.removeContainerCmd(storageMockContainerId).exec();
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
			final StringJoiner storageDriverAddrsOption = new StringJoiner(
				",", "--storage-driver-addrs=", ""
			);
			int nextStorageDriverPort;
			for(int i = 0; i < n; i ++) {
				nextStorageDriverPort = driverConfig.getPort() + i;
				final String[] cmd = {
					"-Xms1g", "-Xmx1g", "-XX:MaxDirectMemorySize=1g",
					"-jar", "/opt/mongoose/mongoose-storage-driver-service.jar",
					"--storage-driver-port=" + nextStorageDriverPort
				};
				final CreateContainerResponse container = dockerClient
					.createContainerCmd(STORAGE_DRIVER_IMAGE_NAME)
					.withName("mongoose_storage_driver_service_" + nextStorageDriverPort)
					.withNetworkMode("host")
					.withExposedPorts(ExposedPort.tcp(nextStorageDriverPort))
					.withEntrypoint("java")
					.withCmd(cmd)
					.exec();
				final String containerId = container.getId();
				dockerClient.startContainerCmd(containerId).exec();
				Loggers.TEST.info(
					"Started the storage driver service @ port #{} in the container {}",
					nextStorageDriverPort, containerId
				);
				storageDriverBuilderSvcs.add(containerId);
				storageDriverAddrsOption.add("127.0.0.1:" + nextStorageDriverPort);
			}
			configArgs.add(storageDriverAddrsOption.toString());
			configArgs.add("--storage-driver-remote");
		}
	}

	private void tearDownDistributedModeIfNeeded()
	throws Exception {
		if(driverCount.equals(DriverCount.DISTRIBUTED) && storageDriverBuilderSvcs != null) {
			for(final String svcContainerId : storageDriverBuilderSvcs) {
				dockerClient.killContainerCmd(svcContainerId).exec();
				dockerClient.removeContainerCmd(svcContainerId).exec();
			}
			storageDriverBuilderSvcs.clear();
		}
	}

}
