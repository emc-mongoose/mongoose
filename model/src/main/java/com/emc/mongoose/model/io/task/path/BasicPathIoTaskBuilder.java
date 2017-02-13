package com.emc.mongoose.model.io.task.path;

import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import static com.emc.mongoose.model.io.task.path.PathIoTask.PathIoResult;
import com.emc.mongoose.model.item.PathItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 30.01.17.
 */
public class BasicPathIoTaskBuilder<
	I extends PathItem, O extends PathIoTask<I, R>, R extends PathIoResult<I>
>
extends BasicIoTaskBuilder<I, O, R>
implements PathIoTaskBuilder<I, O, R> {
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I dataItem, final String dstPath)
	throws IOException {
		return (O) new BasicPathIoTask<>(originCode, ioType, dataItem);
	}
	
	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		for(final I nextItem : items) {
			tasks.add((O) new BasicPathIoTask<>(originCode, ioType, nextItem));
		}
		return tasks;
	}
	
	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items, final String dstPath)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		for(final I nextItem : items) {
			tasks.add((O) new BasicPathIoTask<>(originCode, ioType, nextItem));
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
			tasks.add((O) new BasicPathIoTask<>(originCode, ioType, nextItem));
		}
		return tasks;
	}
}
