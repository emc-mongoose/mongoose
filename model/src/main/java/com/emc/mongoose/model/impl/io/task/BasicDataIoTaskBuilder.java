package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.data.DataRangesConfig;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.DataIoTaskBuilder;
import com.emc.mongoose.model.api.item.DataItem;

/**
 Created by kurila on 14.07.16.
 */
public class BasicDataIoTaskBuilder<I extends DataItem, O extends DataIoTask<I>>
extends BasicIoTaskBuilder<I, O>
implements DataIoTaskBuilder<I, O> {
	
	protected volatile DataRangesConfig rangesConfig = null;
	
	@Override
	public BasicDataIoTaskBuilder<I, O> setRangesConfig(final DataRangesConfig rangesConfig) {
		this.rangesConfig = rangesConfig;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I dataItem, final String dstPath) {
		return (O) new BasicDataIoTask<>(ioType, dataItem, dstPath, rangesConfig);
	}
}
