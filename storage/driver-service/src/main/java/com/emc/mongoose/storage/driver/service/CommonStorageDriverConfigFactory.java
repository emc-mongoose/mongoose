package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.model.api.StorageType;
import com.emc.mongoose.storage.driver.fs.FsStorageDriverConfigFactory;
import com.emc.mongoose.storage.driver.http.base.HttpStorageDriverConfigFactory;

/**
 Created on 25.09.16.
 */
public interface CommonStorageDriverConfigFactory
extends FsStorageDriverConfigFactory, HttpStorageDriverConfigFactory {

	StorageType getStorageType();

}
