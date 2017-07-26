package com.emc.mongoose.api.common.concurrent;

import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 26.07.17.
 */
public abstract class CoroutineBase
extends StoppableTaskBase
implements Coroutine {

	private final List<Coroutine> coroutineRegistry;

	protected CoroutineBase(final List<Coroutine> coroutineRegistry) {
		this.coroutineRegistry = coroutineRegistry;
	}

	@Override
	public final void close()
	throws IOException {
		coroutineRegistry.remove(this);
		super.close();
	}
}
