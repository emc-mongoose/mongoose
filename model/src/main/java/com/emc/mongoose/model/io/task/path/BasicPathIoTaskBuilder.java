package com.emc.mongoose.model.io.task.path;

import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.item.PathItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 30.01.17.
 */
public class BasicPathIoTaskBuilder<I extends PathItem, O extends PathIoTask<I>>
extends BasicIoTaskBuilder<I, O>
implements PathIoTaskBuilder<I, O> {
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I pathItem)
	throws IOException {
		final String uid;
		return (O) new BasicPathIoTask<>(
			originCode, ioType, pathItem, uid = getNextUid(), getNextSecret(uid)
		);
	}
	
	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		String uid;
		for(final I nextItem : items) {
			tasks.add(
				(O) new BasicPathIoTask<>(
					originCode, ioType, nextItem, uid = getNextUid(), getNextSecret(uid)
				)
			);
		}
		return tasks;
	}
}
