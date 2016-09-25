package com.emc.mongoose.monitor.driver.api;

import com.emc.mongoose.model.util.SizeInBytes;

/**
 Created on 25.09.16.
 */
public interface FsDriverConfigFactory
extends DriverConfigFactory {

	SizeInBytes getIoBuffSize();

}
