package com.emc.mongoose.model.io.task.token;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.BasicIoTask;
import com.emc.mongoose.model.item.TokenItem;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static java.lang.System.nanoTime;

/**
 Created by kurila on 20.10.15.
 */
public class BasicTokenIoTask<I extends TokenItem>
extends BasicIoTask<I>
implements TokenIoTask<I> {
	
	protected transient volatile long countBytesDone;
	protected transient volatile long respDataTimeStart;
	
	public BasicTokenIoTask() {
	}

	public BasicTokenIoTask(final int originCode, final IoType ioType, final I item) {
		super(originCode, ioType, item, null, null);
	}

	public static class BasicTokenIoResult<I extends TokenItem>
	extends BasicIoResult<I>
	implements TokenIoResult<I> {
		
		private long dataLatency;
		private long transferredByteCount;
		
		public BasicTokenIoResult() {
		}
		
		public BasicTokenIoResult(
			final int originCode,
			final String storageDriverAddr, final String storageNodeAddr, final I item,
			final int ioTypeCode, final int statusCode, final long reqTimeStart,
			final long duration, final long latency, final long dataLatency,
			final long transferredByteCount
		) {
			super(
				originCode,
				storageDriverAddr, storageNodeAddr, item, ioTypeCode, statusCode, reqTimeStart,
				duration, latency
			);
			this.dataLatency = dataLatency;
			this.transferredByteCount = transferredByteCount;
		}
		
		@Override
		public final long getDataLatency() {
			return dataLatency;
		}
		
		@Override
		public final long getCountBytesDone() {
			return transferredByteCount;
		}
		
		@Override
		public void writeExternal(final ObjectOutput out)
		throws IOException {
			super.writeExternal(out);
			out.writeLong(dataLatency);
			out.writeLong(transferredByteCount);
		}
		
		@Override
		public void readExternal(final ObjectInput in)
		throws IOException, ClassNotFoundException {
			super.readExternal(in);
			dataLatency = in.readLong();
			transferredByteCount = in.readLong();
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
			originCode,
			useStorageDriverResult ? hostAddr : null,
			useStorageNodeResult ? nodeAddr : null,
			useItemInfoResult ? item : null,
			useIoTypeCodeResult ? ioType.ordinal() : -1,
			useStatusCodeResult ? status.ordinal() : -1,
			useReqTimeStartResult ? reqTimeStart : -1,
			useDurationResult ? respTimeDone - reqTimeStart : -1,
			useRespLatencyResult ? respTimeStart - reqTimeDone : -1,
			useDataLatencyResult ? respDataTimeStart - reqTimeDone : -1,
			useTransferSizeResult ? countBytesDone : -1
		);
	}
	
	@Override
	public long getCountBytesDone() {
		return countBytesDone;
	}
	
	@Override
	public void setCountBytesDone(final long n) {
		this.countBytesDone = n;
	}
	
	@Override
	public long getRespDataTimeStart() {
		return respDataTimeStart;
	}
	
	@Override
	public void startDataResponse() {
		respDataTimeStart = START_OFFSET_MICROS + nanoTime() / 1000;
	}
}
