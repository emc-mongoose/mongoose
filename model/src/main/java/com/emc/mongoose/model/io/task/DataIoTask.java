package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.item.DataItem;

/**
 Created by kurila on 11.07.16.
 */
public interface DataIoTask<D extends DataItem>
extends IoTask<D> {

	interface DataIoResult
	extends IoResult {

		long getDataLatency();

		long getCountBytesDone();
	}

	@Override
	D getItem();

	long getCountBytesDone();

	void setCountBytesDone(long n);

	boolean isResponseDataStarted();

	void startDataResponse();
}
