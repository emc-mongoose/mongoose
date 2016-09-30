package com.emc.mongoose.storage.driver.service;

import com.emc.mongoose.common.pattern.FactorySvc;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.storage.driver.base.StorageDriverConfigFactory;

/**
 Created on 28.09.16.
 */
public interface StorageDriverFactorySvc<
	I extends Item & DataItem & MutableDataItem,
	O extends IoTask<I> & DataIoTask<I> & MutableDataIoTask<I>,
	T extends StorageDriverConfigFactory
	>
extends FactorySvc<String, T> {

	String SVC_NAME = StorageDriverFactorySvc.class.getCanonicalName();

}
