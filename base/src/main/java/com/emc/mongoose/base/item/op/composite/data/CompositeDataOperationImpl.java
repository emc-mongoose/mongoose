package com.emc.mongoose.base.item.op.composite.data;

import com.emc.mongoose.base.item.op.data.DataOperationImpl;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.partial.data.PartialDataOperation;
import com.emc.mongoose.base.item.op.partial.data.PartialDataOperationImpl;
import com.emc.mongoose.base.storage.Credential;
import com.github.akurilov.commons.collection.Range;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Created by andrey on 25.11.16. */
public class CompositeDataOperationImpl<I extends DataItem> extends DataOperationImpl<I>
				implements CompositeDataOperation<I> {

	private long sizeThreshold;
	private final AtomicInteger pendingSubTasksCount = new AtomicInteger(-1);

	private final Map<String, String> contextData = new HashMap<>();
	private final List<PartialDataOperation<I>> subTasks = new ArrayList<>();

	public CompositeDataOperationImpl() {
		super();
	}

	public CompositeDataOperationImpl(
					final int originIndex,
					final OpType opType,
					final I item,
					final String srcPath,
					final String dstPath,
					final Credential credential,
					final List<Range> fixedRanges,
					final int randomRangesCount,
					final long sizeThreshold) {
		super(originIndex, opType, item, srcPath, dstPath, credential, fixedRanges, randomRangesCount);
		this.sizeThreshold = sizeThreshold;
	}

	protected CompositeDataOperationImpl(final CompositeDataOperationImpl<I> other) {
		super(other);
		this.sizeThreshold = other.sizeThreshold;
		this.pendingSubTasksCount.set(other.pendingSubTasksCount.get());
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
	public final List<? extends PartialDataOperation<I>> subOperations() {

		if (!subTasks.isEmpty()) {
			return subTasks;
		}

		final int equalPartsCount = sizeThreshold > 0 ? (int) (contentSize / sizeThreshold) : 0;
		final long tailPartSize = contentSize % sizeThreshold;
		I nextPart;
		PartialDataOperation<I> nextSubTask;
		for (int i = 0; i < equalPartsCount; i++) {
			nextPart = item.slice(i * sizeThreshold, sizeThreshold);
			nextSubTask = new PartialDataOperationImpl<>(
							originIndex, opType, nextPart, srcPath, dstPath, credential, i, this);
			nextSubTask.srcPath(srcPath);
			subTasks.add(nextSubTask);
		}
		if (tailPartSize > 0) {
			nextPart = item.slice(equalPartsCount * sizeThreshold, tailPartSize);
			nextSubTask = new PartialDataOperationImpl<>(
							originIndex, opType, nextPart, srcPath, dstPath, credential, equalPartsCount, this);
			nextSubTask.srcPath(srcPath);
			subTasks.add(nextSubTask);
		}

		pendingSubTasksCount.set(subTasks.size());

		return subTasks;
	}

	@Override
	public final void markSubTaskCompleted() {
		pendingSubTasksCount.decrementAndGet();
	}

	@Override
	public final boolean allSubOperationsDone() {
		return pendingSubTasksCount.get() == 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final CompositeDataOperationImpl<I> result() {
		buildItemPath(item, dstPath == null ? srcPath : dstPath);
		return new CompositeDataOperationImpl<>(this);
	}
}
