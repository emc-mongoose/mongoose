package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.storage.driver.base.StorageDriverConfigFactory;
import com.emc.mongoose.ui.config.Config.SocketConfig;

/**
 Created on 25.09.16.
 */
public interface HttpStorageDriverConfigFactory
extends StorageDriverConfigFactory {

	SocketConfig getSocketConfig();

	boolean getVerifyFlag();

}
