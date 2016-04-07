package com.emc.mongoose.core.api.load.generator;
//
import com.emc.mongoose.common.conf.enums.LoadType;
//
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;

/**
 Created by andrey on 08.04.16.
 */
public interface IoTaskGenerator<T extends Item, A extends IoTask<T>>
extends ItemGenerator<T> {
	//
	IoConfig<? extends Item, ? extends Container<? extends Item>> getIoConfig();
	//
	LoadType getLoadType();
	//
	LoadExecutor<T> getLoadExecutor();
}
