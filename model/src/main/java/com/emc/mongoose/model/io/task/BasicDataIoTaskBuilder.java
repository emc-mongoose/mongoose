package com.emc.mongoose.model.io.task;

import com.emc.mongoose.common.api.ByteRange;
import static com.emc.mongoose.model.io.task.DataIoTask.DataIoResult;
import com.emc.mongoose.model.item.DataItem;

import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public class BasicDataIoTaskBuilder<
	I extends DataItem, O extends DataIoTask<I, R>, R extends DataIoResult
>
extends BasicIoTaskBuilder<I, O, R>
implements DataIoTaskBuilder<I, O, R> {

	protected volatile List<ByteRange> fixedRanges = null;
	protected volatile int randomRangesCount = 0;
	protected volatile long sizeThreshold = 0;

	@Override
	public BasicDataIoTaskBuilder<I, O, R> setFixedRanges(final List<ByteRange> fixedRanges) {
		this.fixedRanges = fixedRanges;
		return this;
	}
	@Override
	public BasicDataIoTaskBuilder<I, O, R> setRandomRangesCount(final int count) {
		this.randomRangesCount = count;
		return this;
	}
	@Override
	public BasicDataIoTaskBuilder<I, O, R> setSizeThreshold(final long sizeThreshold) {
		this.sizeThreshold = sizeThreshold;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I dataItem, final String dstPath) {
		return (O) new BasicDataIoTask<>(
			ioType, dataItem, srcPath, dstPath, fixedRanges, randomRangesCount, sizeThreshold
		);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final int from, final int to) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add(
				(O) new BasicDataIoTask<>(
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
				(O) new BasicDataIoTask<>(
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
				(O) new BasicDataIoTask<>(
					ioType, items.get(i), srcPath, dstPaths.get(i), fixedRanges, randomRangesCount,
					sizeThreshold
				)
			);
		}
		return tasks;
	}
}
