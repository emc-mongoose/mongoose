package com.emc.mongoose.model.io.task.token;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.BasicIoTask;
import com.emc.mongoose.model.io.task.token.TokenIoTask.TokenIoResult;
import com.emc.mongoose.model.item.TokenItem;

/**
 Created by kurila on 20.10.15.
 */
public class BasicTokenIoTask<I extends TokenItem, R extends TokenIoResult>
extends BasicIoTask<I, R>
implements TokenIoTask<I, R> {

	public BasicTokenIoTask() {
	}

	public BasicTokenIoTask(final IoType ioType, final I item) {
		super(ioType, item, null, null);
	}

	public static class BasicTokenIoResult<I extends TokenItem>
	extends BasicIoResult<I>
	implements TokenIoResult<I> {
		
		private String storageDriverAddr;
		private String storageNodeAddr;
		private I item;
		private int ioTypeCode;
		private int statusCode;
		private long reqTimeStart;
		private long duration;
		private long latency;

		public BasicTokenIoResult() {
		}
		
		public BasicTokenIoResult(
			final String storageDriverAddr, final String storageNodeAddr, final I item,
			final int ioTypeCode, final int statusCode, final long reqTimeStart,
			final long duration, final long latency
		) {
			super(
				storageDriverAddr, storageNodeAddr, item, ioTypeCode, statusCode, reqTimeStart,
				duration, latency
			);
		}
	}
	
	
	@Override @SuppressWarnings("unchecked")
	public R getResult(
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
	) {
		//buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return (R) new BasicTokenIoResult(
			useStorageDriverResult ? hostAddr : null,
			useStorageNodeResult ? nodeAddr : null,
			useItemInfoResult ? item : null,
			useIoTypeCodeResult ? ioType.ordinal() : -1,
			useStatusCodeResult ? status.ordinal() : -1,
			useReqTimeStartResult ? reqTimeStart : -1,
			useDurationResult ? respTimeDone - reqTimeStart : -1,
			useRespLatencyResult ? respTimeStart - reqTimeDone : -1
		);
	}
}
