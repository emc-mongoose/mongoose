package com.emc.mongoose.storage.driver.fs;

import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.storage.driver.base.StorageDriverConfigFactory;

/**
 Created on 25.09.16.
 */
public interface FsStorageDriverConfigFactory
extends StorageDriverConfigFactory {

	SizeInBytes getIoBuffSize();

}
