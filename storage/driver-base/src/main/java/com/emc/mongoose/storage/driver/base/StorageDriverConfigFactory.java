package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.config.Config.StorageConfig;

import java.io.Serializable;

/**
 Created on 25.09.16.
 */
public interface StorageDriverConfigFactory
extends Serializable {

	String getRunId();
	LoadConfig getLoadConfig();
	String getSourceContainer();
	StorageConfig getStorageConfig();
	boolean getVerifyFlag();

}
