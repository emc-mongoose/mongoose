package com.emc.mongoose.common.io;

import com.emc.mongoose.common.item.DataItem;

/**
 Created by kurila on 11.07.16.
 */
public interface DataIoTask<D extends DataItem>
extends IoTask<D> {

	long getCountBytesDone();

	int getDataLatency();
}
