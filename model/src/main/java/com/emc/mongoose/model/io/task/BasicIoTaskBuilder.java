package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.io.task.result.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public class BasicIoTaskBuilder<I extends Item, R extends IoResult, O extends IoTask<I, R>>
implements IoTaskBuilder<I, R, O> {
	
	protected volatile IoType ioType = IoType.CREATE; // by default
	protected volatile String srcPath = null;

	@Override
	public final IoType getIoType() {
		return ioType;
	}

	@Override
	public final BasicIoTaskBuilder<I, R, O> setIoType(final IoType ioType) {
		this.ioType = ioType;
		return this;
	}

	@Override
	public final String getSrcPath() {
		return srcPath;
	}

	@Override
	public final BasicIoTaskBuilder<I, R, O> setSrcPath(final String srcPath) {
		this.srcPath = srcPath;
		return this;
	}

	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I item, final String dstPath) {
		return (O) new BasicIoTask<>(ioType, item, srcPath, dstPath);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final int from, final int to) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add((O) new BasicIoTask<>(ioType, items.get(i), srcPath, null));
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(
		final List<I> items, final String dstPath, final int from, final int to
	) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add((O) new BasicIoTask<>(ioType, items.get(i), srcPath, dstPath));
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(
		final List<I> items, final List<String> dstPaths, final int from, final int to
	) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add((O) new BasicIoTask<>(ioType, items.get(i), srcPath, dstPaths.get(i)));
		}
		return tasks;
	}
}
