package com.emc.mongoose.model.io.task.partial.data.mutable;

import static com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask.PartialDataIoResult;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.composite.data.mutable.CompositeMutableDataIoTask;
import com.emc.mongoose.model.io.task.data.mutable.BasicMutableDataIoTask;
import static com.emc.mongoose.model.io.task.partial.data.BasicPartialDataIoTask.BasicPartialDataIoResult;
import com.emc.mongoose.model.item.MutableDataItem;

/**
 Created by andrey on 25.11.16.
 */
public class BasicPartialMutableDataIoTask<I extends MutableDataItem, R extends PartialDataIoResult>
extends BasicMutableDataIoTask<I, R>
implements PartialMutableDataIoTask<I, R> {

	private final int partNumber;
	private final CompositeMutableDataIoTask<I, ? extends DataIoResult> parent;

	public BasicPartialMutableDataIoTask(
		final IoType ioType, final I part, final String srcPath, final String dstPath,
		final int partNumber, final CompositeMutableDataIoTask<I, ? extends DataIoResult> parent
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
	public final CompositeMutableDataIoTask<I, ? extends DataIoResult> getParent() {
		return parent;
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
