package com.emc.mongoose.model.io.task.data.mutable;

import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import com.emc.mongoose.model.io.task.composite.data.mutable.BasicCompositeMutableDataIoTask;
import com.emc.mongoose.model.io.task.data.BasicDataIoTaskBuilder;
import com.emc.mongoose.model.item.MutableDataItem;

import java.io.IOException;
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
	public final O getInstance(final I item, final String dstPath)
	throws IOException {
		if(item.size() > sizeThreshold) {
			return (O) new BasicCompositeMutableDataIoTask<>(
				ioType, item, srcPath, dstPath, fixedRanges, randomRangesCount, sizeThreshold
			);
		} else {
			return (O) new BasicMutableDataIoTask<>(
				ioType, item, srcPath, dstPath, fixedRanges, randomRangesCount
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
					(O) new BasicCompositeMutableDataIoTask<>(
						ioType, nextItem, srcPath, null, fixedRanges, randomRangesCount,
						sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicMutableDataIoTask<>(
						ioType, nextItem, srcPath, null, fixedRanges, randomRangesCount
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
					(O) new BasicCompositeMutableDataIoTask<>(
						ioType, nextItem, srcPath, dstPath, fixedRanges, randomRangesCount,
						sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicMutableDataIoTask<>(
						ioType, nextItem, srcPath, dstPath, fixedRanges, randomRangesCount
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
					(O) new BasicCompositeMutableDataIoTask<>(
						ioType, nextItem, srcPath, dstPaths.get(i), fixedRanges, randomRangesCount,
						sizeThreshold
					)
				);
			} else {
				tasks.add(
					(O) new BasicMutableDataIoTask<>(
						ioType, nextItem, srcPath, dstPaths.get(i), fixedRanges, randomRangesCount
					)
				);
			}
		}
		return tasks;
	}
}
