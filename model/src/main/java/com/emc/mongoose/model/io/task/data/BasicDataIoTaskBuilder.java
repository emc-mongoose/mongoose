package com.emc.mongoose.model.io.task.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.model.io.task.IoTaskBuilderBase;
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
extends IoTaskBuilderBase<I, O, R>
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
				originCode, ioType, dataItem, srcPath, dstPath, fixedRanges, randomRangesCount,
				sizeThreshold
			);
		} else {
			return (O) new BasicDataIoTask<>(
				originCode, ioType, dataItem, srcPath, dstPath, fixedRanges, randomRangesCount
			);
		}
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		for(final I nextItem : items) {
			if(nextItem.size() > sizeThreshold) {
				tasks.add(
					(O) new BasicCompositeDataIoTask<>(
						originCode, ioType, nextItem, srcPath, null, fixedRanges, randomRangesCount,
						sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicDataIoTask<>(
						originCode, ioType, nextItem, srcPath, null, fixedRanges, randomRangesCount
					)
				);
			}
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final String dstPath)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		for(final I nextItem : items) {
			if(nextItem.size() > sizeThreshold) {
				tasks.add(
					(O) new BasicCompositeDataIoTask<>(
						originCode, ioType, nextItem, srcPath, dstPath, fixedRanges,
						randomRangesCount, sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicDataIoTask<>(
						originCode, ioType, nextItem, srcPath, dstPath, fixedRanges,
						randomRangesCount
					)
				);
			}
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final List<String> dstPaths)
	throws IOException {
		final int n = items.size();
		if(dstPaths.size() != n) {
			throw new IllegalArgumentException("Items count and paths count should be equal");
		}
		final List<O> tasks = new ArrayList<>(n);
		I nextItem;
		for(int i = 0; i < n; i ++) {
			nextItem = items.get(i);
			if(nextItem.size() > sizeThreshold) {
				tasks.add(
					(O) new BasicCompositeDataIoTask<>(
						originCode, ioType, nextItem, srcPath, dstPaths.get(i), fixedRanges,
						randomRangesCount, sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicDataIoTask<>(
						originCode, ioType, nextItem, srcPath, dstPaths.get(i), fixedRanges,
						randomRangesCount
					)
				);
			}
		}
		return tasks;
	}
}
