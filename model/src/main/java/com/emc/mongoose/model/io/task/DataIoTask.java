package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.io.task.DataIoTask.DataIoResult;

import java.util.List;

/**
 Created by kurila on 11.07.16.
 */
public interface DataIoTask<D extends DataItem, R extends DataIoResult>
extends IoTask<D, R> {
	
	interface DataIoResult
	extends IoTask.IoResult {
		
		long getDataLatency();
		
		long getCountBytesDone();
	}
	
	@Override D getItem();

	boolean isMultiPart();

	List<D> getParts();

	long getCountBytesDone();

	void setCountBytesDone(long n);

	long getRespDataTimeStart();

	void startDataResponse();
}
