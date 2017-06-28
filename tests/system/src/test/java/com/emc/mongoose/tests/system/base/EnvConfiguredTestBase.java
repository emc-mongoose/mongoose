package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.storage.driver.service.BasicStorageDriverBuilderSvc;
import com.emc.mongoose.storage.mock.impl.http.StorageMockFactory;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NetConfig.NodeConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.NamingConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.MockConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.MockConfig.ContainerConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.MockConfig.FailConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NetConfig;
import com.emc.mongoose.ui.log.Loggers;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 Created by andrey on 04.06.17.
 */
public class EnvConfiguredTestBase
extends ConfiguredTestBase {

	// environment parameter names
	public static final String KEY_ENV_STORAGE_DRIVER_TYPE = "STORAGE_DRIVER_TYPE";
	public static final String KEY_ENV_STORAGE_DRIVER_COUNT = "STORAGE_DRIVER_COUNT";
	public static final String KEY_ENV_STORAGE_DRIVER_CONCURRENCY = "STORAGE_DRIVER_CONCURRENCY";
	public static final String KEY_ENV_ITEM_DATA_SIZE = "ITEM_DATA_SIZE";

	// parameter values to be set
	protected static String STORAGE_DRIVER_TYPE = null;
	protected static boolean DISTRIBUTED_MODE_FLAG = false;
	protected static int CONCURRENCY = 0;
	protected static SizeInBytes ITEM_DATA_SIZE = null;

	// test exclusion mechanism
	protected static final Map<String, List<Object>> EXCLUDE_PARAMS = new HashMap<>();
	protected static boolean SKIP_FLAG = false;

	protected static Map<String, Daemon> HTTP_STORAGE_MOCKS = null;
	protected static int HTTP_STORAGE_NODE_COUNT = 1;
	protected static int STORAGE_DRIVERS_COUNT;
	private static List<StorageDriverBuilderSvc> STORAGE_DRIVER_BUILDER_SVCS = null;
	protected static final String STORAGE_TYPE_FS = "fs";
	protected static final String STORAGE_TYPE_ATMOS = "atmos";
	protected static final String STORAGE_TYPE_S3 = "s3";
	protected static final String STORAGE_TYPE_SWIFT = "swift";

	@BeforeClass
	public static void setUpClass()
	throws Exception {

		ConfiguredTestBase.setUpClass();
		final Map<String, String> env = System.getenv();

		if(!env.containsKey(KEY_ENV_STORAGE_DRIVER_TYPE)) {
			throw new IllegalArgumentException(
				"Environment property w/ name \"" + KEY_ENV_STORAGE_DRIVER_TYPE +
					"\" is not defined"
			);
		}
		STORAGE_DRIVER_TYPE = System.getenv(KEY_ENV_STORAGE_DRIVER_TYPE);
		checkExclusionAndSetFlag(KEY_ENV_STORAGE_DRIVER_TYPE, STORAGE_DRIVER_TYPE);

		if(!env.containsKey(KEY_ENV_STORAGE_DRIVER_COUNT)) {
			throw new IllegalArgumentException(
				"Environment property w/ name \"" + KEY_ENV_STORAGE_DRIVER_COUNT +
					"\" is not defined"
			);
		}
		STORAGE_DRIVERS_COUNT = Integer.parseInt(System.getenv(KEY_ENV_STORAGE_DRIVER_COUNT));
		DISTRIBUTED_MODE_FLAG = STORAGE_DRIVERS_COUNT > 1;
		checkExclusionAndSetFlag(KEY_ENV_STORAGE_DRIVER_COUNT, DISTRIBUTED_MODE_FLAG);

		if(!env.containsKey(KEY_ENV_STORAGE_DRIVER_CONCURRENCY)) {
			throw new IllegalArgumentException(
				"Environment property w/ name \"" + KEY_ENV_STORAGE_DRIVER_CONCURRENCY +
					"\" is not defined"
			);
		}
		CONCURRENCY = Integer.parseInt(System.getenv(KEY_ENV_STORAGE_DRIVER_CONCURRENCY));
		checkExclusionAndSetFlag(KEY_ENV_STORAGE_DRIVER_CONCURRENCY, CONCURRENCY);

		if(!env.containsKey(KEY_ENV_ITEM_DATA_SIZE)) {
			throw new IllegalArgumentException(
				"Environment property w/ name \"" + KEY_ENV_ITEM_DATA_SIZE + "\" is not defined"
			);
		}
		ITEM_DATA_SIZE = new SizeInBytes(System.getenv(KEY_ENV_ITEM_DATA_SIZE));
		checkExclusionAndSetFlag(KEY_ENV_ITEM_DATA_SIZE, ITEM_DATA_SIZE);

		Loggers.MSG.info("* Storage type:     {}", STORAGE_DRIVER_TYPE);
		Loggers.MSG.info("* Distributed mode: {}", DISTRIBUTED_MODE_FLAG);
		Loggers.MSG.info("* Concurrency:      {}", CONCURRENCY);
		Loggers.MSG.info("* Items size:       {}", ITEM_DATA_SIZE);
		Loggers.MSG.info("* Excluded:         {}", SKIP_FLAG);

		if(SKIP_FLAG) {
			return;
		}

		setUpStorageMockIfNeeded();
		setUpDistributedModeIfNeeded();
		setUpConcurrency();
		setUpItemDataSize();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		if(! SKIP_FLAG) {
			tearDownStorageMockIfNeeded();
			tearDownDistributedModeIfNeeded();
		}
		ConfiguredTestBase.tearDownClass();
	}

	private static void checkExclusionAndSetFlag(final String name, final Object value) {
		final List<Object> excludeParams = EXCLUDE_PARAMS.get(name);
		if(excludeParams != null && !excludeParams.isEmpty()) {
			for(final Object nextExcludeParam : excludeParams) {
				if(nextExcludeParam.equals(value)) {
					System.out.println("Test excluded for the " + name + "=" + value);
					SKIP_FLAG = true;
					break;
				}
			}
		}
	}

	private static void setUpStorageMockIfNeeded()
	throws Exception {
		final StorageConfig storageConfig = CONFIG.getStorageConfig();
		storageConfig.getDriverConfig().setType(STORAGE_DRIVER_TYPE);
		switch(STORAGE_DRIVER_TYPE) {
			case STORAGE_TYPE_ATMOS:
			case STORAGE_TYPE_S3:
			case STORAGE_TYPE_SWIFT:
				HTTP_STORAGE_MOCKS = new HashMap<>();
				final NodeConfig nodeConfig = storageConfig.getNetConfig().getNodeConfig();
				final ItemConfig itemConfig = CONFIG.getItemConfig();
				final StepConfig stepConfig = CONFIG.getTestConfig().getStepConfig();
				final int port = nodeConfig.getPort();
				final List<String> nodeAddrs = new ArrayList<>();
				String nextNodeAddr;
				Daemon storageMock;
				final MockConfig mockConfig = storageConfig.getMockConfig();
				final NetConfig netConfig = storageConfig.getNetConfig();
				final ContainerConfig containerConfig = mockConfig.getContainerConfig();
				final FailConfig failConfig = mockConfig.getFailConfig();
				final NamingConfig namingConfig = itemConfig.getNamingConfig();
				final DataConfig.ContentConfig contentConfig = itemConfig.getDataConfig().getContentConfig();
				final ContentSource contentSrc = ContentSourceUtil.getInstance(
					contentConfig.getFile(), contentConfig.getSeed(),
					contentConfig.getRingConfig().getSize(),
					contentConfig.getRingConfig().getCache()
				);
				StorageMockFactory storageMockFactory;
				for(int i = 0; i < HTTP_STORAGE_NODE_COUNT; i ++) {
					nodeConfig.setPort(port + i);
					storageMockFactory = new StorageMockFactory(
						itemConfig.getInputConfig().getFile(), mockConfig.getCapacity(),
						containerConfig.getCapacity(), containerConfig.getCountLimit(),
						(int) CONFIG.getOutputConfig().getMetricsConfig().getAverageConfig().getPeriod(),
						failConfig.getConnections(), failConfig.getResponses(), contentSrc,
						netConfig.getNodeConfig().getPort(), netConfig.getSsl(),
						(float) stepConfig.getLimitConfig().getRate(), namingConfig.getPrefix(),
						namingConfig.getRadix()
					);
					storageMock = storageMockFactory.newStorageMock();
					nextNodeAddr = "127.0.0.1:" + (port + i);
					storageMock.start();
					HTTP_STORAGE_MOCKS.put(nextNodeAddr, storageMock);
					nodeAddrs.add(nextNodeAddr);
				}
				nodeConfig.setAddrs(nodeAddrs);
				nodeConfig.setPort(port);
				break;
			case STORAGE_TYPE_FS:
				break;
		}
	}

	private static void tearDownStorageMockIfNeeded()
	throws Exception {
		if(HTTP_STORAGE_MOCKS != null) {
			for(final Daemon storageMock : HTTP_STORAGE_MOCKS.values()) {
				storageMock.close();
			}
			HTTP_STORAGE_MOCKS.clear();
			HTTP_STORAGE_MOCKS = null;
		}
	}

	private static void setUpDistributedModeIfNeeded()
	throws Exception {
		if(DISTRIBUTED_MODE_FLAG) {
			STORAGE_DRIVER_BUILDER_SVCS = new ArrayList<>(STORAGE_DRIVERS_COUNT);
			final DriverConfig driverConfig = CONFIG.getStorageConfig().getDriverConfig();
			final List<String> storageDriverAddrs = new ArrayList<>(STORAGE_DRIVERS_COUNT);
			int nextStorageDriverPort;
			StorageDriverBuilderSvc nextStorageDriverBuilder;
			for(int i = 0; i < STORAGE_DRIVERS_COUNT; i ++) {
				nextStorageDriverPort = driverConfig.getPort() + i;
				nextStorageDriverBuilder = new BasicStorageDriverBuilderSvc(nextStorageDriverPort);
				nextStorageDriverBuilder.start();
				STORAGE_DRIVER_BUILDER_SVCS.add(nextStorageDriverBuilder);
				storageDriverAddrs.add("127.0.0.1:" + nextStorageDriverPort);
			}
			driverConfig.setAddrs(storageDriverAddrs);
			driverConfig.setRemote(true);
		}
	}

	private static void tearDownDistributedModeIfNeeded()
	throws Exception {
		if(DISTRIBUTED_MODE_FLAG && STORAGE_DRIVER_BUILDER_SVCS != null) {
			for(final StorageDriverBuilderSvc svc : STORAGE_DRIVER_BUILDER_SVCS) {
				svc.close();
			}
			STORAGE_DRIVER_BUILDER_SVCS.clear();
			STORAGE_DRIVER_BUILDER_SVCS = null;
		}
	}

	private static void setUpConcurrency()
	throws Exception {
		if(CONCURRENCY < 1) {
			throw new IllegalArgumentException("Concurrency level should be an integer > 0");
		}
		CONFIG.getStorageConfig().getDriverConfig().setConcurrency(CONCURRENCY);
	}

	private static void setUpItemDataSize()
	throws Exception {
		CONFIG.getItemConfig().getDataConfig().setSize(ITEM_DATA_SIZE);
	}
}
