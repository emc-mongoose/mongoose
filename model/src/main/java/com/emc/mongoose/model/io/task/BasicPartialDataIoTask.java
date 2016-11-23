package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.result.BasicPartialDataIoResult;
import com.emc.mongoose.model.io.task.result.PartialDataIoResult;
import com.emc.mongoose.model.item.DataItem;

/**
 Created by andrey on 23.11.16.
 */
public class BasicPartialDataIoTask<I extends DataItem, R extends PartialDataIoResult>
extends BasicDataIoTask<I, R>
implements PartialDataIoTask<I, R> {

	private final int partNumber;

	public BasicPartialDataIoTask(
		final IoType ioType, final I part, final String srcPath, final String dstPath,
		final int partNumber
	) {
		super(ioType, part, srcPath, dstPath, null, 0, 0);
		this.partNumber = partNumber;
	}

	@Override
	public final int getPartNumber() {
		return partNumber;
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
		return (R) new BasicPartialDataIoResult(
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
}
