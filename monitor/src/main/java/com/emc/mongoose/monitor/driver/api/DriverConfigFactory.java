package com.emc.mongoose.monitor.driver.api;

import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;

/**
 Created on 25.09.16.
 */
public interface DriverConfigFactory {

	String getRunId();
	LoadConfig getLoadConfig();
	String getSourceContainer();
	StorageConfig getStorageConfig();

}
