package com.emc.mongoose.model.io.task;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.io.task.result.BasicDataIoResult;
import com.emc.mongoose.model.io.task.result.DataIoResult;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.io.IoType;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class BasicDataIoTask<T extends DataItem, R extends DataIoResult>
extends BasicIoTask<T, R>
implements DataIoTask<T, R> {

	protected long contentSize;
	protected long sizeThreshold;
	protected long itemDataOffset;

	protected transient volatile ContentSource contentSrc;
	protected transient volatile long countBytesDone;
	protected transient volatile long respDataTimeStart;
	
	public BasicDataIoTask() {
		super();
	}
	
	public BasicDataIoTask(
		final IoType ioType, final T item, final String srcPath, final String dstPath,
		final List<ByteRange> fixedRanges, final int randomRangesCount, final long sizeThreshold
	) {
		super(ioType, item, srcPath, dstPath);
		this.sizeThreshold = sizeThreshold;
		item.reset();
		//currDataLayerIdx = item.getCurrLayerIndex();
		switch(ioType) {
			case CREATE:
			case READ:
				// TODO partial read support, use rangesConfig
				try {
					contentSize = item.size();
				} catch(IOException e) {
					throw new IllegalStateException();
				}
				break;
			default:
				contentSize = 0;
				break;
		}
		itemDataOffset = item.offset();
		contentSrc = item.getContentSrc();
	}

	@Override @SuppressWarnings("unchecked")
	public R getResult(
		final String hostAddr,
		final boolean useStorageDriverResult,
		final boolean useStorageNodeResult,
		final boolean useItemPathResult,
		final boolean useIoTypeCodeResult,
		final boolean useStatusCodeResult,
		final boolean useReqTimeStartResult,
		final boolean useDurationResult,
		final boolean useRespLatencyResult,
		final boolean useDataLatencyResult,
		final boolean useTransferSizeResult
	) {
		return (R) new BasicDataIoResult(
			useStorageDriverResult ? hostAddr : null,
			useStorageNodeResult ? nodeAddr : null,
			useItemPathResult ? getItemPath(item.getName(), srcPath, dstPath) : null,
			useIoTypeCodeResult ? ioType.ordinal() : - 1,
			useStatusCodeResult ? status.ordinal() : - 1,
			useReqTimeStartResult ? reqTimeStart : - 1,
			useDurationResult ? respTimeDone - reqTimeStart : - 1,
			useRespLatencyResult ? respTimeStart - reqTimeDone : - 1,
			useDataLatencyResult ? respDataTimeStart - reqTimeDone : - 1,
			useTransferSizeResult ? countBytesDone : -1
		);
	}

	@Override
	public void reset() {
		super.reset();
		countBytesDone = 0;
		respDataTimeStart = 0;
	}

	@Override
	public final List<DataItem> getParts() {
		final List<DataItem> parts = new ArrayList<>();
		final int equalPartsCount = sizeThreshold > 0 ? (int) (contentSize / sizeThreshold) : 0;
		final long tailPartSize = contentSize % sizeThreshold;
		DataItem nextPart;
		for(int i = 0; i < equalPartsCount; i ++) {
			nextPart = item.slice(i * sizeThreshold, sizeThreshold);
			parts.add(nextPart);
		}
		if(tailPartSize > 0) {
			nextPart = item.slice(equalPartsCount * sizeThreshold , tailPartSize);
			parts.add(nextPart);
		}
		return parts;
	}

	@Override
	public final boolean isMultiPart() {
		return contentSize > sizeThreshold || sizeThreshold > 0;
	}

	@Override
	public final long getCountBytesDone() {
		return countBytesDone;
	}

	@Override
	public final void setCountBytesDone(final long n) {
		this.countBytesDone = n;
	}

	@Override
	public final long getRespDataTimeStart() {
		return respDataTimeStart;
	}

	@Override
	public final void startDataResponse() {
		respDataTimeStart = System.nanoTime() / 1000;
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(contentSize);
		out.writeLong(sizeThreshold);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		itemDataOffset = item.offset();
		contentSrc = item.getContentSrc();
		contentSize = in.readLong();
		sizeThreshold = in.readLong();
	}
}
