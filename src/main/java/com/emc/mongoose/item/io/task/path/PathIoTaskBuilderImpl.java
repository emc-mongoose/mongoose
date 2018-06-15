package com.emc.mongoose.item.io.task.path;

import com.emc.mongoose.item.io.task.IoTaskBuilderImpl;
import com.emc.mongoose.item.PathItem;
import com.emc.mongoose.storage.Credential;

import java.io.IOException;
import java.util.List;

/**
 Created by kurila on 30.01.17.
 */
public class PathIoTaskBuilderImpl<I extends PathItem, O extends PathIoTask<I>>
extends IoTaskBuilderImpl<I, O>
implements PathIoTaskBuilder<I, O> {

	public PathIoTaskBuilderImpl(final int originIndex) {
		super(originIndex);
	}

	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I pathItem)
	throws IOException {
		final String uid;
		return (O) new PathIoTaskImpl<>(
			originIndex, ioType, pathItem,
			Credential.getInstance(uid = getNextUid(), getNextSecret(uid))
		);
	}
	
	@Override @SuppressWarnings("unchecked")
	public void getInstances(final List<I> items, final List<O> buff)
	throws IOException {
		String uid;
		for(final I nextItem : items) {
			buff.add(
				(O) new PathIoTaskImpl<>(
					originIndex, ioType, nextItem,
					Credential.getInstance(uid = getNextUid(), getNextSecret(uid))
				)
			);
		}
	}
}
