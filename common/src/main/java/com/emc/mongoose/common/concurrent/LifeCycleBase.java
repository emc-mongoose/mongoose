package com.emc.mongoose.common.concurrent;

import java.util.concurrent.atomic.AtomicReference;

/**
 Created on 12.07.16.
 */
public abstract class LifeCycleBase
implements LifeCycle {

	private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

	private enum State {
		INIT, STARTED, SHUTDOWN, INTERRUPTED
	}

	protected abstract void doStart();

	protected abstract void doShutdown();

	protected abstract void doInterrupt();

	@Override
	public final void start()
	throws IllegalStateException {
	}

	@Override
	public final void shutdown()
	throws IllegalStateException {
	}

	@Override
	public final void interrupt()
	throws IllegalStateException {
	}
}
