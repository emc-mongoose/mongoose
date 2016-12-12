package com.emc.mongoose.model.io.task.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.io.task.composite.data.BasicCompositeDataIoTask;
import com.emc.mongoose.model.item.DataItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public class BasicDataIoTaskBuilder<
	I extends DataItem, O extends DataIoTask<I, R>, R extends DataIoTask.DataIoResult
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
		this.sizeThreshold = sizeThreshold > 0 ? sizeThreshold : Long.MAX_VALUE;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I dataItem, final String dstPath)
	throws IOException {
		if(dataItem.size() > sizeThreshold) {
			return (O) new BasicCompositeDataIoTask<>(
				ioType, dataItem, srcPath, dstPath, fixedRanges, randomRangesCount, sizeThreshold
			);
		} else {
			return (O) new BasicDataIoTask<>(
				ioType, dataItem, srcPath, dstPath, fixedRanges, randomRangesCount
			);
		}
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final int from, final int to)
	throws IOException {
		final List<O> tasks = new ArrayList<>(to - from);
		I nextItem;
		for(int i = from; i < to; i ++) {
			nextItem = items.get(i);
			if(nextItem.size() > sizeThreshold) {
				tasks.add(
					(O) new BasicCompositeDataIoTask<>(
						ioType, nextItem, srcPath, null, fixedRanges, randomRangesCount,
						sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicDataIoTask<>(
						ioType, nextItem, srcPath, null, fixedRanges, randomRangesCount
					)
				);
			}
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(
		final List<I> items, final String dstPath, final int from, final int to
	) throws IOException {
		final List<O> tasks = new ArrayList<>(to - from);
		I nextItem;
		for(int i = from; i < to; i ++) {
			nextItem = items.get(i);
			if(nextItem.size() > sizeThreshold) {
				tasks.add(
					(O) new BasicCompositeDataIoTask<>(
						ioType, nextItem, srcPath, dstPath, fixedRanges, randomRangesCount,
						sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicDataIoTask<>(
						ioType, nextItem, srcPath, dstPath, fixedRanges, randomRangesCount
					)
				);
			}
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(
		final List<I> items, final List<String> dstPaths, final int from, final int to
	) throws IOException {
		final List<O> tasks = new ArrayList<>(to - from);
		I nextItem;
		for(int i = from; i < to; i ++) {
			nextItem = items.get(i);
			if(nextItem.size() > sizeThreshold) {
				tasks.add(
					(O) new BasicCompositeDataIoTask<>(
						ioType, nextItem, srcPath, dstPaths.get(i - from), fixedRanges,
						randomRangesCount, sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicDataIoTask<>(
						ioType, nextItem, srcPath, dstPaths.get(i - from), fixedRanges,
						randomRangesCount
					)
				);
			}
		}
		return tasks;
	}
}
