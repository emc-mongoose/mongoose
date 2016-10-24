package com.emc.mongoose.model.impl.io.task;

import com.emc.mongoose.model.api.io.task.MutableDataIoTask;
import com.emc.mongoose.model.api.io.task.MutableDataIoTaskBuilder;
import com.emc.mongoose.model.api.item.MutableDataItem;

import java.util.ArrayList;
import java.util.List;
/**
 Created by andrey on 25.09.16.
 */
public class BasicMutableDataIoTaskBuilder<I extends MutableDataItem, O extends MutableDataIoTask<I>>
extends BasicDataIoTaskBuilder<I, O>
implements MutableDataIoTaskBuilder<I, O> {
	
	@Override @SuppressWarnings("unchecked")
	public final O getInstance(final I item, final String dstPath) {
		return (O) new BasicMutableDataIoTask<>(ioType, item, srcPath, dstPath, rangesConfig);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final int from, final int to) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add(
				(O) new BasicMutableDataIoTask<>(ioType, items.get(i), srcPath, null, rangesConfig)
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
					ioType, items.get(i), srcPath, dstPath, rangesConfig
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
					ioType, items.get(i), srcPath, dstPaths.get(i), rangesConfig
				)
			);
		}
		return tasks;
	}
}
