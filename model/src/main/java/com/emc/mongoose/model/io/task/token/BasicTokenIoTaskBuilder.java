package com.emc.mongoose.model.io.task.token;

import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import static com.emc.mongoose.model.io.task.token.TokenIoTask.TokenIoResult;
import com.emc.mongoose.model.item.TokenItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public class BasicTokenIoTaskBuilder<
	I extends TokenItem, O extends TokenIoTask<I, R>, R extends TokenIoResult<I>
>
extends BasicIoTaskBuilder<I, O, R>
implements TokenIoTaskBuilder<I, O, R> {

	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I item, final String dstPath)
	throws IOException {
		return (O) new BasicTokenIoTask<>(originCode, ioType, item);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		for(final I item : items) {
			tasks.add((O) new BasicTokenIoTask<>(originCode, ioType, item));
		}
		return tasks;
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final String dstPath)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		for(final I item : items) {
			tasks.add((O) new BasicTokenIoTask<>(originCode, ioType, item));
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
		for(int i = 0; i < n; i ++) {
			tasks.add((O) new BasicTokenIoTask<>(originCode, ioType, items.get(i)));
		}
		return tasks;
	}
}
