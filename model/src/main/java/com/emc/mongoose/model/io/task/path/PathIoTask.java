package com.emc.mongoose.model.io.task.path;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.PathItem;

/**
 Created by kurila on 30.01.17.
 */
public interface PathIoTask<I extends PathItem, R extends PathIoTask.PathIoResult<I>>
extends IoTask<I, R> {
	
	interface PathIoResult<I extends PathItem>
	extends IoResult<I> {
		
		@Override
		I getItem();
		
		long getDataLatency();
		
		long getCountBytesDone();
	}
	
	@Override
	I getItem();
	
	@Override
	R getResult(
		final String hostAddr,
		final boolean useStorageDriverResult,
		final boolean useStorageNodeResult,
		final boolean useItemInfoResult,
		final boolean useIoTypeCodeResult,
		final boolean useStatusCodeResult,
		final boolean useReqTimeStartResult,
		final boolean useDurationResult,
		final boolean useRespLatencyResult,
		final boolean useDataLatencyResult,
		final boolean useTransferSizeResult
	);
	
	long getCountBytesDone();
	
	void setCountBytesDone(long n);
	
	long getRespDataTimeStart();
	
	void startDataResponse();
}
