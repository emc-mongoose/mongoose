package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.exception.UserShootHisFootException;

import java.util.concurrent.atomic.AtomicReference;

/**
 Created on 12.07.16.
 */
public abstract class InterruptibleDaemonBase
implements InterruptibleDaemon {

	private AtomicReference<State> stateRef = new AtomicReference<>(State.INITIAL);

	private enum State {
		INITIAL, STARTED, SHUTDOWN, INTERRUPTED
	}

	protected abstract void doStart()
	throws UserShootHisFootException;

	protected abstract void doShutdown()
	throws UserShootHisFootException;

	protected abstract void doInterrupt()
	throws UserShootHisFootException;

	@Override
	public final void start()
	throws UserShootHisFootException {
		if(stateRef.compareAndSet(State.INITIAL, State.STARTED)) {
			doStart();
		} else {
			throw new UserShootHisFootException("start failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isStarted() {
		return stateRef.get().equals(State.STARTED);
	}

	@Override
	public final void shutdown()
	throws UserShootHisFootException {
		if(stateRef.compareAndSet(State.INITIAL, State.SHUTDOWN)) {
			doShutdown();
		} else if(stateRef.compareAndSet(State.STARTED, State.SHUTDOWN)) {
			doShutdown();
		} else {
			throw new UserShootHisFootException("shutdown failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isShutdown() {
		return stateRef.get().equals(State.SHUTDOWN);
	}

	@Override
	public final void interrupt()
	throws UserShootHisFootException {
		try {
			shutdown();
		} catch(final UserShootHisFootException ignored) {
		}
		if(stateRef.compareAndSet(State.SHUTDOWN, State.INTERRUPTED)) {
			doInterrupt();
		} else {
			throw new UserShootHisFootException("interrupt failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isInterrupted() {
		return stateRef.get().equals(State.INTERRUPTED);
	}
}
