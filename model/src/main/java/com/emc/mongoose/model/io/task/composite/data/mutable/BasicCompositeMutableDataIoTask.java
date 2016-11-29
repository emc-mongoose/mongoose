package com.emc.mongoose.model.io.task.composite.data.mutable;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.io.IoType;
import static com.emc.mongoose.model.io.task.composite.data.CompositeDataIoTask.CompositeDataIoResult;
import static com.emc.mongoose.model.io.task.composite.data.BasicCompositeDataIoTask.BasicCompositeDataIoResult;
import com.emc.mongoose.model.io.task.data.mutable.BasicMutableDataIoTask;
import com.emc.mongoose.model.io.task.partial.data.mutable.BasicPartialMutableDataIoTask;
import com.emc.mongoose.model.io.task.partial.data.mutable.PartialMutableDataIoTask;
import com.emc.mongoose.model.item.MutableDataItem;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by andrey on 25.11.16.
 */
public class BasicCompositeMutableDataIoTask<
	I extends MutableDataItem, R extends CompositeDataIoResult
>
extends BasicMutableDataIoTask<I, R>
implements CompositeMutableDataIoTask<I, R> {

	private long sizeThreshold;

	private transient final Map<String, String> contextData = new HashMap<>();
	private transient final List<PartialMutableDataIoTask> subTasks = new ArrayList<>();
	private transient AtomicInteger pendingSubTasksCount = new AtomicInteger(-1);

	public BasicCompositeMutableDataIoTask() {
		super();
	}

	public BasicCompositeMutableDataIoTask(
		final IoType ioType, final I item, final String srcPath, final String dstPath,
		final List<ByteRange> fixedRanges, final int randomRangesCount, final long sizeThreshold
	) {
		super(ioType, item, srcPath, dstPath, fixedRanges, randomRangesCount);
		this.sizeThreshold = sizeThreshold;
	}

	@Override
	public final String get(final String key) {
		return contextData.get(key);
	}

	@Override
	public final void put(final String key, final String value) {
		contextData.put(key, value);
	}

	@Override
	public final List<PartialMutableDataIoTask> getSubTasks() {

		if(!subTasks.isEmpty()) {
			return subTasks;
		}

		final int equalPartsCount = sizeThreshold > 0 ? (int) (contentSize / sizeThreshold) : 0;
		final long tailPartSize = contentSize % sizeThreshold;
		I nextPart;
		PartialMutableDataIoTask nextSubTask;
		for(int i = 0; i < equalPartsCount; i ++) {
			nextPart = item.slice(i * sizeThreshold, sizeThreshold);
			nextSubTask = new BasicPartialMutableDataIoTask<>(
				ioType, nextPart, srcPath, dstPath, i, this
			);
			subTasks.add(nextSubTask);
		}
		if(tailPartSize > 0) {
			nextPart = item.slice(equalPartsCount * sizeThreshold , tailPartSize);
			nextSubTask = new BasicPartialMutableDataIoTask<>(
				ioType, nextPart, srcPath, dstPath, equalPartsCount, this
			);
			subTasks.add(nextSubTask);
		}

		pendingSubTasksCount.set(subTasks.size());

		return subTasks;
	}

	@Override
	public final void subTaskCompleted() {
		pendingSubTasksCount.decrementAndGet();
	}

	@Override
	public final boolean allSubTasksDone() {
		return pendingSubTasksCount.get() == 0;
	}
	
	@Override @SuppressWarnings("unchecked")
	public final R getResult(
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
		return (R) new BasicCompositeDataIoResult(
			useStorageDriverResult ? hostAddr : null,
			useStorageNodeResult ? nodeAddr : null,
			useItemPathResult ? getItemPath(item.getName(), srcPath, dstPath) : null,
			useIoTypeCodeResult ? ioType.ordinal() : - 1,
			useStatusCodeResult ? status.ordinal() : - 1,
			useReqTimeStartResult ? reqTimeStart : - 1,
			useDurationResult ? respTimeDone - reqTimeStart : - 1,
			useRespLatencyResult ? respTimeStart - reqTimeDone : - 1,
			useDataLatencyResult ? respDataTimeStart - reqTimeDone : - 1,
			useTransferSizeResult ? 0 : -1,
			allSubTasksDone()
		);
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeLong(sizeThreshold);
	}

	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		sizeThreshold = in.readLong();
	}
}
