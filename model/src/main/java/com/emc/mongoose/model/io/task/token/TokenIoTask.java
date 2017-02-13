package com.emc.mongoose.model.io.task.token;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.TokenItem;

/**
 Created by kurila on 11.07.16.
 */
public interface TokenIoTask<I extends TokenItem, R extends TokenIoTask.TokenIoResult<I>>
extends IoTask<I, R> {

	interface TokenIoResult<I extends TokenItem>
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
