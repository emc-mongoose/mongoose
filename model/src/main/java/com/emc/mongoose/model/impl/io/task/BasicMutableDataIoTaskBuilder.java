package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.io.task.MutableDataIoTaskBuilder;
import com.emc.mongoose.model.api.item.MutableDataItem;

/**
 Created by andrey on 25.09.16.
 */
public class BasicMutableDataIoTaskBuilder<I extends MutableDataItem, O extends MutableDataIoTask<I>>
extends BasicDataIoTaskBuilder<I, O>
implements MutableDataIoTaskBuilder<I, O> {
	
	@Override @SuppressWarnings("unchecked")
	public final O getInstance(final I item, final String dstPath) {
		return (O) new BasicMutableDataIoTask<I>(ioType, item, dstPath, rangesConfig);
	}
}
