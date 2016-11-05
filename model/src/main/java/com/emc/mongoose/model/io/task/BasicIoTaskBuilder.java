package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadType;

import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public class BasicIoTaskBuilder<I extends Item, O extends IoTask<I>>
implements IoTaskBuilder<I, O> {
	
	protected volatile LoadType ioType = LoadType.CREATE; // by default
	protected volatile String srcPath = null;
	protected volatile String authId = null;
	protected volatile String secret = null;
	
	@Override
	public final BasicIoTaskBuilder<I, O> setIoType(final LoadType ioType) {
		this.ioType = ioType;
		return this;
	}

	@Override
	public final BasicIoTaskBuilder<I, O> setSrcPath(final String srcPath) {
		this.srcPath = srcPath;
		return this;
	}

	@Override
	public final BasicIoTaskBuilder<I, O> setAuthId(final String authId) {
		this.authId = authId;
		return this;
	}

	@Override
	public final BasicIoTaskBuilder<I, O> setSecret(final String secret) {
		this.secret = secret;
		return this;
	}

	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I item, final String dstPath) {
		return (O) new BasicIoTask<>(ioType, item, srcPath, dstPath, authId, secret);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final int from, final int to) {
		final List<O> tasks = new ArrayList<>(to - from);
		for(int i = from; i < to; i ++) {
			tasks.add((O) new BasicIoTask<>(ioType, items.get(i), srcPath, null, authId, secret));
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
				(O) new BasicIoTask<>(ioType, items.get(i), srcPath, dstPath, authId, secret)
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
				(O) new BasicIoTask<>(
					ioType, items.get(i), srcPath, dstPaths.get(i), authId, secret
				)
			);
		}
		return tasks;
	}
}
