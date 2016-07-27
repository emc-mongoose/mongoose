package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.IoTaskFactory;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.util.LoadType;

/**
 Created by kurila on 14.07.16.
 */
public final class BasicIoTaskFactory<I extends Item>
implements IoTaskFactory<I, BasicIoTask<I>> {
	
	@Override
	public final BasicIoTask<I> getInstance(
		final LoadType ioType, final I item, final String dstPath
	) {
		return new BasicIoTask<>(ioType, item, dstPath);
	}
}
