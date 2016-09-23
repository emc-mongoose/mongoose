package com.emc.mongoose.common.concurrent;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created on 12.07.16.
 */
public abstract class DaemonBase
implements Daemon {

	private AtomicReference<State> stateRef = new AtomicReference<>(State.INITIAL);
	protected final Object state = new Object();

	private enum State {
		INITIAL, STARTED, SHUTDOWN, INTERRUPTED, CLOSED
	}

	protected abstract void doStart()
	throws IllegalStateException;

	protected abstract void doShutdown()
	throws IllegalStateException;

	protected abstract void doInterrupt()
	throws IllegalStateException;
	
	protected abstract void doClose()
	throws IOException, IllegalStateException;

	@Override
	public final void start()
	throws IllegalStateException {
		if(stateRef.compareAndSet(State.INITIAL, State.STARTED)) {
			synchronized(state) {
				state.notifyAll();
			}
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
		if(
			stateRef.compareAndSet(State.INITIAL, State.SHUTDOWN) ||
				stateRef.compareAndSet(State.STARTED, State.SHUTDOWN)
			) {
			synchronized(state) {
				state.notifyAll();
			}
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
	public final void await()
	throws InterruptedException, RemoteException {
		await(Long.MAX_VALUE, TimeUnit.SECONDS);
	}
	
	@Override
	public final void interrupt()
	throws IllegalStateException {
		try {
			shutdown();
		} catch(final IllegalStateException ignored) {
		}
		if(stateRef.compareAndSet(State.SHUTDOWN, State.INTERRUPTED)) {
			synchronized(state) {
				state.notifyAll();
			}
			doInterrupt();
		} else {
			throw new IllegalStateException("interrupt failed: state is " + stateRef.get());
		}
	}

	@Override
	public final boolean isInterrupted() {
		return stateRef.get().equals(State.INTERRUPTED);
	}
	
	@Override
	public final void close()
	throws IOException, IllegalStateException {
		try {
			interrupt();
		} catch(final IllegalStateException ignored) {
		}
		if(stateRef.compareAndSet(State.INTERRUPTED, State.CLOSED)) {
			synchronized(state) {
				state.notifyAll();
			}
			doClose();
		} else {
			throw new IllegalStateException("close failed: state is " + stateRef.get());
		}
	}
	
	@Override
	public final boolean isClosed() {
		return stateRef.get().equals(State.CLOSED);
	}
}
