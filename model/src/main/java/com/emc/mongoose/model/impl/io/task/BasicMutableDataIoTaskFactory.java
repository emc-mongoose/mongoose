package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.IoTaskFactory;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.util.LoadType;

import java.io.IOException;
/**
 Created by andrey on 25.09.16.
 */
public class BasicMutableDataIoTaskFactory<M extends MutableDataItem>
implements IoTaskFactory<M, BasicMutableDataIoTask<M>> {

	@Override
	public final BasicMutableDataIoTask<M> getInstance(
		final LoadType ioType, final M item, final String dstPath
	) {
		try {
			return new BasicMutableDataIoTask<>(ioType, item, dstPath);
		} catch(final IOException ignored) {
			return null;
		}
	}
}
