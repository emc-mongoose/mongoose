package com.emc.mongoose.io;

import com.emc.mongoose.item.DataItem;

/**
 Created by kurila on 11.07.16.
 */
public interface DataIoTask<D extends DataItem>
extends IoTask<D> {

	long getCountBytesDone();

	int getDataLatency();
}
