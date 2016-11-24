package com.emc.mongoose.model.io.task;

import static com.emc.mongoose.model.io.task.DataIoTask.DataIoResult;
import com.emc.mongoose.model.item.MutableDataItem;

import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 25.09.16.
 */
public class BasicMutableDataIoTaskBuilder<
	I extends MutableDataItem, O extends MutableDataIoTask<I, R>, R extends DataIoResult
>
extends BasicDataIoTaskBuilder<I, O, R>
implements MutableDataIoTaskBuilder<I, O, R> {
	
	@Override @SuppressWarnings("unchecked")
	public final O getInstance(final I item, final String dstPath) {
		return (O) new BasicMutableDataIoTask<>(
			ioType, item, srcPath, dstPath, fixedRanges, randomRangesCount, sizeThreshold
		);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final int from, final int to) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add(
				(O) new BasicMutableDataIoTask<>(
					ioType, items.get(i), srcPath, null, fixedRanges, randomRangesCount,
					sizeThreshold
				)
			);
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(
		final List<I> items, final String dstPath, final int from, final int to
	) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add(
				(O) new BasicMutableDataIoTask<>(
					ioType, items.get(i), srcPath, dstPath, fixedRanges, randomRangesCount,
					sizeThreshold
				)
			);
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(
		final List<I> items, final List<String> dstPaths, final int from, final int to
	) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add(
				(O) new BasicMutableDataIoTask<>(
					ioType, items.get(i), srcPath, dstPaths.get(i), fixedRanges, randomRangesCount,
					sizeThreshold
				)
			);
		}
		return tasks;
	}
}
