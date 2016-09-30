package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.SocketConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig.StorageType;

/**
 Created on 30.09.16.
 */
public class BasicStorageDriverConfigFactory
implements CommonStorageDriverConfigFactory {

	private final StorageType storageType;
	private final String runId;
	private final LoadConfig loadConfig;
	private final String srcContainer;
	private final StorageConfig storageConfig;
	private final boolean verifyFlag;

	private SocketConfig socketConfig;
	private SizeInBytes ioBuffSize;

	public BasicStorageDriverConfigFactory(
		final StorageType storageType, final String runId, final LoadConfig loadConfig,
		final String srcContainer, final StorageConfig storageConfig, final boolean verifyFlag,
		final SocketConfig socketConfig
	) {
		this.storageType = storageType;
		this.runId = runId;
		this.loadConfig = loadConfig;
		this.srcContainer = srcContainer;
		this.storageConfig = storageConfig;
		this.verifyFlag = verifyFlag;
		this.socketConfig = socketConfig;
	}

	public BasicStorageDriverConfigFactory(
		final StorageType storageType, final String runId, final LoadConfig loadConfig,
		final String srcContainer, final StorageConfig storageConfig, final boolean verifyFlag,
		final SizeInBytes ioBuffSize
	) {
		this.storageType = storageType;
		this.runId = runId;
		this.loadConfig = loadConfig;
		this.srcContainer = srcContainer;
		this.storageConfig = storageConfig;
		this.verifyFlag = verifyFlag;
		this.ioBuffSize = ioBuffSize;
	}

	@Override
	public StorageType getStorageType() {
		return storageType;
	}

	@Override
	public SizeInBytes getIoBuffSize() {
		return ioBuffSize;
	}

	@Override
	public SocketConfig getSocketConfig() {
		return socketConfig;
	}

	@Override
	public boolean getVerifyFlag() {
		return verifyFlag;
	}

	@Override
	public String getRunId() {
		return runId;
	}

	@Override
	public LoadConfig getLoadConfig() {
		return loadConfig;
	}

	@Override
	public String getSourceContainer() {
		return srcContainer;
	}

	@Override
	public StorageConfig getStorageConfig() {
		return storageConfig;
	}
}
