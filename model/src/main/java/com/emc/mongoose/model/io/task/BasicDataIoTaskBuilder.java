package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.data.DataRangesConfig;
import com.emc.mongoose.model.item.DataItem;

import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public class BasicDataIoTaskBuilder<I extends DataItem, O extends DataIoTask<I>>
extends BasicIoTaskBuilder<I, O>
implements DataIoTaskBuilder<I, O> {
	
	protected volatile DataRangesConfig rangesConfig = null;
	
	@Override
	public BasicDataIoTaskBuilder<I, O> setRangesConfig(final DataRangesConfig rangesConfig) {
		this.rangesConfig = rangesConfig;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I dataItem, final String dstPath) {
		return (O) new BasicDataIoTask<>(
			ioType, dataItem, srcPath, dstPath, authId, secret, rangesConfig
		);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final int from, final int to) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add(
				(O) new BasicDataIoTask<>(
					ioType, items.get(i), srcPath, null, authId, secret, rangesConfig
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
					ioType, items.get(i), srcPath, dstPath, authId, secret, rangesConfig
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
					ioType, items.get(i), srcPath, dstPaths.get(i), authId, secret, rangesConfig
				)
			);
		}
		return tasks;
	}
}
