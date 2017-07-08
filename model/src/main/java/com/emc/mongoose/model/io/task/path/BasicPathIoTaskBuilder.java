package com.emc.mongoose.model.io.task.path;

import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.item.PathItem;
import com.emc.mongoose.model.storage.Credential;

import java.io.IOException;
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
			originCode, ioType, pathItem,
			Credential.getInstance(uid = getNextUid(), getNextSecret(uid))
		);
	}
	
	@Override @SuppressWarnings("unchecked")
	public void getInstances(final List<I> items, final List<O> buff)
	throws IOException {
		String uid;
		for(final I nextItem : items) {
			buff.add(
				(O) new BasicPathIoTask<>(
					originCode, ioType, nextItem,
					Credential.getInstance(uid = getNextUid(), getNextSecret(uid))
				)
			);
		}
	}
}
