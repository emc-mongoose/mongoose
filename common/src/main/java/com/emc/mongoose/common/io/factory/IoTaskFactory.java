package com.emc.mongoose.common.io.factory;

import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;

/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskFactory<I extends Item, O extends IoTask<I>> {

	O getInstance(final LoadType ioType, final I item);
	
}
