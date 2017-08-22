package com.emc.mongoose.api.model.concurrent;

import com.github.akurilov.coroutines.CoroutinesProcessor;

import static com.emc.mongoose.api.model.concurrent.Daemon.State.CLOSED;
import static com.emc.mongoose.api.model.concurrent.Daemon.State.INITIAL;
import static com.emc.mongoose.api.model.concurrent.Daemon.State.INTERRUPTED;
import static com.emc.mongoose.api.model.concurrent.Daemon.State.SHUTDOWN;
import static com.emc.mongoose.api.model.concurrent.Daemon.State.STARTED;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created on 12.07.16.
 */
public abstract class DaemonBase
implements Daemon {

	protected static final CoroutinesProcessor SVC_EXECUTOR = new CoroutinesProcessor();

	public static void setThreadCount(final int threadCount) {
		SVC_EXECUTOR.setThreadCount(threadCount);
	}

	private static final List<Daemon> REGISTRY = new ArrayList<>();
	
	private AtomicReference<State> stateRef = new AtomicReference<>(INITIAL);
	protected final Object state = new Object();

	protected DaemonBase() {
		REGISTRY.add(this);
	}
	
	@Override
	public final State getState() {
		return stateRef.get();
	}

	protected abstract void doStart()
	throws IllegalStateException;

	protected abstract void doShutdown()
	throws IllegalStateException;

	protected abstract void doInterrupt()
	throws IllegalStateException;

	protected abstract void doClose()
	throws IllegalStateException, IOException;

	@Override
	public final void start()
	throws IllegalStateException {
		if(stateRef.compareAndSet(INITIAL, STARTED)) {
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
		return stateRef.get().equals(STARTED);
	}

	@Override
	public final synchronized void shutdown()
	throws IllegalStateException {
		if(stateRef.compareAndSet(INITIAL, SHUTDOWN) || stateRef.compareAndSet(STARTED, SHUTDOWN)) {
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
		return stateRef.get().equals(SHUTDOWN);
	}
	
	@Override
	public final void await()
	throws InterruptedException, RemoteException {
		await(Long.MAX_VALUE, TimeUnit.SECONDS);
	}
	
	@Override
	public final synchronized void interrupt()
	throws IllegalStateException {
		try {
			shutdown();
		} catch(final IllegalStateException ignored) {
		}
		if(stateRef.compareAndSet(SHUTDOWN, INTERRUPTED)) {
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
		return stateRef.get().equals(INTERRUPTED);
	}
	
	@Override
	public final synchronized void close()
	throws IOException, IllegalStateException {
		try {
			interrupt();
		} catch(final IllegalStateException ignored) {
		}
		if(stateRef.compareAndSet(INTERRUPTED, CLOSED)) {
			synchronized(state) {
				state.notifyAll();
			}
			doClose();
			// may be closed by another thread right after the interruption
		} else if(!CLOSED.equals(stateRef.get())) {
			throw new IllegalStateException("close failed: state is " + stateRef.get());
		}
	}
	
	@Override
	public final boolean isClosed() {
		return stateRef.get().equals(CLOSED);
	}

	public static void closeAll() {
		synchronized(REGISTRY) {
			for(final Daemon d : REGISTRY) {
				try {
					d.close();
				} catch(final Throwable t) {
					t.printStackTrace(System.err);
				}
			}
		}
	}
}
