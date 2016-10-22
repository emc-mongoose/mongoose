package com.emc.mongoose.model.api.io.task;

import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.LoadType;

/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskBuilder<I extends Item, O extends IoTask<I>> {

	IoTaskBuilder<I, O> setIoType(final LoadType ioType);

	IoTaskBuilder<I, O> setSrcContainer(final String srcContainer);

	O getInstance(final I item, final String dstPath);
}
