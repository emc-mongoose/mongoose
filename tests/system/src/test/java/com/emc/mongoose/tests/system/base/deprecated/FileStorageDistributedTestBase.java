package com.emc.mongoose.tests.system.base.deprecated;

import com.emc.mongoose.storage.driver.builder.StorageDriverBuilderSvc;
import com.emc.mongoose.storage.driver.service.BasicStorageDriverBuilderSvc;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;

import com.emc.mongoose.tests.system.base.ConfiguredTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 07.02.17.
 */
@Deprecated
public class FileStorageDistributedTestBase
extends FileStorageTestBase {

	protected static final int STORAGE_DRIVERS_COUNT = 2;
	private static final List<StorageDriverBuilderSvc>
		STORAGE_DRIVER_BUILDER_SVCS = new ArrayList<>(STORAGE_DRIVERS_COUNT);

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		FileStorageTestBase.setUpClass();
		final DriverConfig driverConfig = ConfiguredTestBase.CONFIG.getStorageConfig().getDriverConfig();
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

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		FileStorageTestBase.tearDownClass();
		for(final StorageDriverBuilderSvc storageDriverBuilderSvc : STORAGE_DRIVER_BUILDER_SVCS) {
			storageDriverBuilderSvc.close();
		}
		STORAGE_DRIVER_BUILDER_SVCS.clear();
	}
}
