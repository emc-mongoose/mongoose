package com.emc.mongoose.model.api.io.task;

import com.emc.mongoose.model.api.item.DataItem;

/**
 Created by kurila on 11.07.16.
 */
public interface DataIoTask<D extends DataItem>
extends IoTask<D> {

	long getCountBytesDone();

	void setCountBytesDone(long n);
	
	void setRespDataTimeStart(final long respDataTimeStart);

	int getDataLatency();
}
