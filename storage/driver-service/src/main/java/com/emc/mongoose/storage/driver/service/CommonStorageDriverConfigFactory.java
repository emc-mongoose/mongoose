package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.storage.driver.fs.FsStorageDriverConfigFactory;
import com.emc.mongoose.storage.driver.http.base.HttpStorageDriverConfigFactory;
import com.emc.mongoose.ui.config.Config.StorageConfig.StorageType;

/**
 Created on 25.09.16.
 */
public interface CommonStorageDriverConfigFactory
extends FsStorageDriverConfigFactory, HttpStorageDriverConfigFactory {

	StorageType getStorageType();

}
