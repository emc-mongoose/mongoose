package com.emc.mongoose.common.io.factory;

import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.io.BasicDataIoTask;
import com.emc.mongoose.common.item.DataItem;

/**
 Created by kurila on 14.07.16.
 */
public final class BasicDataIoTaskFactory<D extends DataItem>
implements IoTaskFactory<D, BasicDataIoTask<D>> {

	@Override
	public final BasicDataIoTask<D> getInstance(final LoadType ioType, final D dataItem) {
		return new BasicDataIoTask<>(ioType, dataItem);
	}
}
