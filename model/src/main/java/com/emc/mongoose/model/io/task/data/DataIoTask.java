package com.emc.mongoose.model.io.task.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;

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

	List<ByteRange> getFixedRanges();

	long getCountBytesDone();

	void setCountBytesDone(long n);

	long getRespDataTimeStart();

	void startDataResponse();
}
