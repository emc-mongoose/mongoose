package com.emc.mongoose.common.concurrent;

import java.util.concurrent.atomic.AtomicReference;

/**
 Created on 12.07.16.
 */
public abstract class LifeCycleBase
implements LifeCycle {

	private AtomicReference<State> stateRef = new AtomicReference<>(State.INITIAL);

	private enum State {
		INITIAL, STARTED, SHUTDOWN, INTERRUPTED
	}

	protected abstract void doStart();

	protected abstract void doShutdown();

	protected abstract void doInterrupt();

	@Override
	public final void start()
	throws IllegalStateException {
		if(stateRef.compareAndSet(State.INITIAL, State.STARTED)) {
			doStart();
		} else {
			throw new IllegalStateException("start failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isStarted() {
		return stateRef.get().equals(State.STARTED);
	}

	@Override
	public final void shutdown()
	throws IllegalStateException {
		if(stateRef.compareAndSet(State.INITIAL, State.SHUTDOWN)) {
			doShutdown();
		} else if(stateRef.compareAndSet(State.STARTED, State.SHUTDOWN)) {
			doShutdown();
		} else {
			throw new IllegalStateException("shutdown failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isShutdown() {
		return stateRef.get().equals(State.SHUTDOWN);
	}

	@Override
	public final void interrupt()
	throws IllegalStateException {
		try {
			shutdown();
		} catch(final IllegalStateException ignored) {
		}
		if(stateRef.compareAndSet(State.SHUTDOWN, State.INTERRUPTED)) {
			doInterrupt();
		} else {
			throw new IllegalStateException("interrupt failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isInterrupted() {
		return stateRef.get().equals(State.INTERRUPTED);
	}
}
