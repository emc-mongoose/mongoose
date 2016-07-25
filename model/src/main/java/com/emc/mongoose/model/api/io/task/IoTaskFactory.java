package com.emc.mongoose.model.api.io.task;

import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.util.LoadType;

/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskFactory<I extends Item, O extends IoTask<I>> {

	O getInstance(final LoadType ioType, final I item, final String dstPath);
}
