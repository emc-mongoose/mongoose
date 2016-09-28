package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.pattern.Factory;
import com.emc.mongoose.common.pattern.FactorySvc;

/**
 Created on 28.09.16.
 */
public interface StorageDriverFactorySvc
extends FactorySvc<String, CommonStorageDriverConfigFactory> {

	String SVC_NAME = StorageDriverFactorySvc.class.getCanonicalName();

}
