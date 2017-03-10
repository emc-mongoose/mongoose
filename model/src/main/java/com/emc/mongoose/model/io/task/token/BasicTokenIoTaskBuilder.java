package com.emc.mongoose.model.io.task.token;

import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.item.TokenItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public class BasicTokenIoTaskBuilder<I extends TokenItem, O extends TokenIoTask<I>>
extends BasicIoTaskBuilder<I, O>
implements TokenIoTaskBuilder<I, O> {

	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I item)
	throws IOException {
		final String uid;
		return (O) new BasicTokenIoTask<>(
			originCode, ioType, item, uid = getNextUid(), getNextSecret(uid)
		);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		String uid;
		for(final I item : items) {
			tasks.add(
				(O) new BasicTokenIoTask<>(
					originCode, ioType, item, uid = getNextUid(), getNextSecret(uid)
				)
			);
		}
		return tasks;
	}
}
