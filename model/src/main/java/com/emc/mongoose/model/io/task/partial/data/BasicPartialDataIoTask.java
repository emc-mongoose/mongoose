package com.emc.mongoose.model.io.task.partial.data;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask;
import com.emc.mongoose.model.io.task.data.BasicDataIoTask;
import static com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask.PartialDataIoResult;

import com.emc.mongoose.model.item.DataItem;

/**
 Created by andrey on 23.11.16.
 */
public class BasicPartialDataIoTask<I extends DataItem, R extends PartialDataIoResult>
extends BasicDataIoTask<I, R>
implements PartialDataIoTask<I, R> {

	private final int partNumber;
	private CompositeDataIoTask<I, ? extends DataIoResult> parent;

	public BasicPartialDataIoTask(
		final IoType ioType, final I part, final String srcPath, final String dstPath,
		final int partNumber, final CompositeDataIoTask<I, ? extends DataIoResult> parent
	) {
		super(ioType, part, srcPath, dstPath, null, 0);
		this.partNumber = partNumber;
		this.parent = parent;
	}

	@Override
	public final int getPartNumber() {
		return partNumber;
	}

	@Override
	public final CompositeDataIoTask<I, ? extends DataIoResult> getParent() {
		return parent;
	}

	@Override
	public final void finishResponse() {
		super.finishResponse();
		parent.subTaskCompleted();
	}

	public static class BasicPartialDataIoResult
	extends BasicDataIoResult
	implements PartialDataIoResult {

		public BasicPartialDataIoResult() {
			super();
		}

		public BasicPartialDataIoResult(
			final String storageDriverAddr, final String storageNodeAddr, final String itemInfo,
			final int ioTypeCode, final int statusCode, final long reqTimeStart,
			final long duration, final long latency, final long dataLatency,
			final long transferredByteCount
		) {
			super(
				storageDriverAddr, storageNodeAddr, itemInfo, ioTypeCode, statusCode,
				reqTimeStart, duration, latency, dataLatency, transferredByteCount
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
		return (R) new BasicPartialDataIoResult(
			useStorageDriverResult ? hostAddr : null,
			useStorageNodeResult ? nodeAddr : null,
			useItemInfoResult ?
				buildItemInfo(dstPath == null ? srcPath : dstPath, item.toString()) : null,
			useIoTypeCodeResult ? ioType.ordinal() : - 1,
			useStatusCodeResult ? status.ordinal() : - 1,
			useReqTimeStartResult ? reqTimeStart : - 1,
			useDurationResult ? respTimeDone - reqTimeStart : - 1,
			useRespLatencyResult ? respTimeStart - reqTimeDone : - 1,
			useDataLatencyResult ? respDataTimeStart - reqTimeDone : - 1,
			useTransferSizeResult ? countBytesDone : -1
		);
	}
}
