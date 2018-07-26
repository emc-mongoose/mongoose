package com.emc.mongoose.system.base;

import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

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

	private static final String STORAGE_MOCK_IMAGE_NAME = "emcmongoose/nagaina";
	protected static final String MONGOOSE_VERSION = System.getenv("MONGOOSE_VERSION") == null ?
		"latest" : System.getenv("MONGOOSE_VERSION");
	static {
		System.out.println("Mongoose images version: " + MONGOOSE_VERSION);
	}
	protected static final String
		BASE_IMAGE_NAME = "emcmongoose/mongoose:" + MONGOOSE_VERSION;

	protected Map<String, String> httpStorageMocks = null;
	protected int httpStorageNodeCount = 1;
	protected List<String> loadStepSvcs = null;
	protected DockerClient dockerClient = null;

	protected final int storageNodePort;
	protected final String itemInputFile;
	protected final String itemNamingPrefix;
	protected final int itemNamingRadix;
	protected final boolean sslFlag;
	protected final List<String> nodeAddrs = new ArrayList<>();

	protected ContainerizedStorageTestBase(
		final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
		final ItemSize itemSize, final int storageNodePort, final String itemInputFile,
		final String itemNamingPrefix, final int itemNamingRadix, final boolean sslFlag
	) throws Exception {
		super(storageType, runMode, concurrency, itemSize);
		this.storageNodePort = storageNodePort;
		this.itemInputFile = itemInputFile;
		this.itemNamingPrefix = itemNamingPrefix;
		this.itemNamingRadix = itemNamingRadix;
		this.sslFlag = sslFlag;
	}

	@Before
	public void setUp()
	throws Exception {
		super.setUp();
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
		switch(storageType) {
			case ATMOS:
			case S3:
			case SWIFT:
				System.out.println("docker pull " + STORAGE_MOCK_IMAGE_NAME + "...");
				dockerClient
					.pullImageCmd(STORAGE_MOCK_IMAGE_NAME)
					.exec(new PullImageResultCallback())
					.awaitCompletion();
				httpStorageMocks = new HashMap<>();
				for(int i = 0; i < httpStorageNodeCount; i ++) {

					final String nodeAddr = "127.0.0.1:" + (storageNodePort + i);

					final List<String> cmd = new ArrayList<>();
					cmd.add("-Xms1g");
					cmd.add("-Xmx1g");
					cmd.add("-XX:MaxDirectMemorySize=1g");
					cmd.add("-jar");
					cmd.add("/opt/nagaina/nagaina.jar");
					if(itemInputFile != null && !itemInputFile.isEmpty()) {
						cmd.add("--item-input-file=" + itemInputFile);
					}
					if(itemNamingPrefix != null) {
						cmd.add("--item-naming-prefix=" + itemNamingPrefix);
					}
					cmd.add("--item-naming-radix=" + itemNamingRadix);
					/*cmd.add("--storage-mock-capacity=" + mockConfig.getCapacity());
					cmd.add("--storage-mock-container-capacity=" + containerConfig.getCapacity());
					cmd.add("--storage-mock-container-countLimit=" + containerConfig.getCountLimit());
					cmd.add("--storage-mock-fail-connections=" + failConfig.getConnections());
					cmd.add("--storage-mock-fail-responses=" + failConfig.getResponses());*/
					cmd.add("--storage-net-node-port=" + (storageNodePort + i));
					cmd.add("--storage-net-ssl=" + sslFlag);
					/*cmd.add("--test-step-limit-rate=" + rateLimit);
					cmd.add("--test-step-metrics-period=" + metricsPeriod);*/

					final CreateContainerResponse container = dockerClient
						.createContainerCmd(STORAGE_MOCK_IMAGE_NAME)
						.withName("mongoose_storage_mock_" + (storageNodePort + i))
						.withNetworkMode("host")
						.withExposedPorts(ExposedPort.tcp(storageNodePort + i))
						.withEntrypoint("java")
						.withCmd(cmd)
						.exec();

					final String containedId = container.getId();
					dockerClient.startContainerCmd(containedId).exec();

					httpStorageMocks.put(nodeAddr, containedId);
					Loggers.TEST.info(
						"Started the storage mock service @ port #{} in the container {}", (storageNodePort + i),
						containedId
					);
					nodeAddrs.add(nodeAddr);
				}
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
		final int n = runMode.getNodeCount();
		if(n > 1) {
			System.out.println("docker pull " + BASE_IMAGE_NAME + "...");
			dockerClient.pullImageCmd(BASE_IMAGE_NAME)
				.exec(new PullImageResultCallback())
				.awaitCompletion();
			loadStepSvcs = new ArrayList<>(n);
			final int defaultLoadStepSvcPort = BUNDLED_DEFAULTS.intVal("load-step-node-port");
			final StringJoiner loadStepNodeAddrsOption = new StringJoiner(
				",", "--load-step-node-addrs=", ""
			);
			int nextLoadStepSvcPort;
			for(int i = 0; i < n; i ++) {
				nextLoadStepSvcPort = defaultLoadStepSvcPort + i;
				final String[] cmd = {
					"--run-node",
					"--load-step-node-port=" + nextLoadStepSvcPort
				};
				final CreateContainerResponse container = dockerClient
					.createContainerCmd(BASE_IMAGE_NAME)
					.withName("mongoose_node_" + nextLoadStepSvcPort)
					.withNetworkMode("host")
					.withExposedPorts(ExposedPort.tcp(nextLoadStepSvcPort))
					.withEntrypoint("/opt/mongoose/entrypoint-debug.sh")
					.withCmd(cmd)
					.exec();
				final String containerId = container.getId();
				dockerClient.startContainerCmd(containerId).exec();
				Loggers.TEST.info(
					"Started the load step service @ port #{} in the container {}",
					nextLoadStepSvcPort, containerId
				);
				loadStepSvcs.add(containerId);
				loadStepNodeAddrsOption.add("127.0.0.1:" + nextLoadStepSvcPort);
			}
			configArgs.add(loadStepNodeAddrsOption.toString());
		}
	}

	private void tearDownDistributedModeIfNeeded()
	throws Exception {
		if(runMode.equals(RunMode.DISTRIBUTED) && loadStepSvcs != null) {
			for(final String svcContainerId : loadStepSvcs) {
				dockerClient.killContainerCmd(svcContainerId).exec();
				dockerClient.removeContainerCmd(svcContainerId).exec();
			}
			loadStepSvcs.clear();
		}
	}

}
