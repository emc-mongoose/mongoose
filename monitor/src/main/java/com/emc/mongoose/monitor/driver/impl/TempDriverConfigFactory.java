package com.emc.mongoose.monitor.driver.impl;

import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.monitor.driver.api.CommonDriverConfigFactory;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;

/**
 Created on 25.09.16.
 */
public class TempDriverConfigFactory
implements CommonDriverConfigFactory {
	
	@Override
	public String getRunId() {
		return "defaultRunId";
	}

	@Override
	public LoadConfig getLoadConfig() {
		return new LoadConfig();
	}

	@Override
	public String getSourceContainer() {
		return "defaultSrcContainer";
	}

	@Override
	public StorageConfig getStorageConfig() {
		return new StorageConfig();
	}

	@Override
	public SocketConfig getSocketConfig() {
		return new SocketConfig();
	}

	@Override
	public SizeInBytes getIoBuffSize() {
		return new SizeInBytes(0, 0, 1);
	}
}
