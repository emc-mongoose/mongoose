package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.io.task.IoTaskBuilder;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.util.LoadType;

/**
 Created by kurila on 14.07.16.
 */
public class BasicIoTaskBuilder<I extends Item, O extends IoTask<I>>
implements IoTaskBuilder<I, O> {
	
	protected volatile LoadType ioType = LoadType.CREATE; // by default
	
	@Override
	public final BasicIoTaskBuilder<I, O> setIoType(final LoadType ioType) {
		this.ioType = ioType;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I item, final String dstPath) {
		return (O) new BasicIoTask<>(ioType, item, dstPath);
	}
}
