package com.emc.mongoose.item.io.task.token;

import com.emc.mongoose.item.io.task.IoTaskBuilderImpl;
import com.emc.mongoose.item.TokenItem;
import com.emc.mongoose.storage.Credential;

import java.io.IOException;
import java.util.List;

/**
 Created by kurila on 14.07.16.
 */
public class TokenIoTaskBuilderImpl<I extends TokenItem, O extends TokenIoTask<I>>
extends IoTaskBuilderImpl<I, O>
implements TokenIoTaskBuilder<I, O> {

	public TokenIoTaskBuilderImpl(final int originIndex) {
		super(originIndex);
	}

	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I item)
	throws IOException {
		final String uid;
		return (O) new TokenIoTaskImpl<>(
			originIndex, ioType, item, Credential.getInstance(uid = getNextUid(), getNextSecret(uid))
		);
	}

	@Override @SuppressWarnings("unchecked")
	public void getInstances(final List<I> items, final List<O> buff)
	throws IOException {
		String uid;
		for(final I item : items) {
			buff.add(
				(O) new TokenIoTaskImpl<>(
					originIndex, ioType, item,
					Credential.getInstance(uid = getNextUid(), getNextSecret(uid))
				)
			);
		}
	}
}
