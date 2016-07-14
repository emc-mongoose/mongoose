package com.emc.mongoose.common.io.factory;

import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.io.BasicIoTask;
import com.emc.mongoose.common.item.Item;

/**
 Created by kurila on 14.07.16.
 */
public final class BasicIoTaskFactory<I extends Item>
implements IoTaskFactory<I, BasicIoTask<I>> {
	
	@Override
	public final BasicIoTask<I> getInstance(final LoadType ioType, final I item) {
		return new BasicIoTask<>(ioType, item);
	}
}
