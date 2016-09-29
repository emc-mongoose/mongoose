package com.emc.mongoose.monitor.driver.config;

import com.emc.mongoose.model.api.storage.StorageType;
import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.ui.config.Config;

/**
 Created on 28.09.16.
 */
public class BasicStorageDriverConfigFactory
	implements CommonStorageDriverConfigFactory {
	
	@Override
	public StorageType getStorageType() {
		return null;
	}

	@Override
	public SizeInBytes getIoBuffSize() {
		return null;
	}

	@Override
	public Config.SocketConfig getSocketConfig() {
		return null;
	}

	@Override
	public String getRunId() {
		return null;
	}

	@Override
	public Config.LoadConfig getLoadConfig() {
		return null;
	}

	@Override
	public String getSourceContainer() {
		return null;
	}

	@Override
	public Config.StorageConfig getStorageConfig() {
		return null;
	}
}
