package com.emc.mongoose.api.model.concurrent;

import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.FINISHED;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.INITIAL;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.STARTED;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.STOPPED;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public abstract class AsyncRunnableBase
implements AsyncRunnable {

	private static final Logger LOG = Logger.getLogger(AsyncRunnableBase.class.getName());

	private final AtomicReference<State> stateRef = new AtomicReference<>(INITIAL);
	private final Object state = new Object();

	@Override
	public final State state() {
		return stateRef.get();
	}

	@Override
	public boolean isInitial() {
		return INITIAL.equals(stateRef.get());
	}

	@Override
	public boolean isStarted() {
		return STARTED.equals(stateRef.get());
	}

	@Override
	public boolean isStopped() {
		return STOPPED.equals(stateRef.get());
	}

	@Override
	public boolean isFinished() {
		return FINISHED.equals(stateRef.get());
	}

	@Override
	public final AsyncRunnableBase start()
	throws IllegalStateException {
		boolean passFlag = stateRef.compareAndSet(INITIAL, STARTED);
		if(!passFlag) {
			passFlag = stateRef.compareAndSet(STOPPED, STARTED);
		}
		if(passFlag) {
			doStart();
			synchronized(state) {
				state.notifyAll();
			}
		} else {
			throw new IllegalStateException(
				"Not allowed to start while state is \"" + stateRef.get() + "\""
			);
		}
		return this;
	}

	@Override
	public final AsyncRunnableBase stop()
	throws IllegalStateException, RemoteException {
		if(stateRef.compareAndSet(STARTED, STOPPED)) {
			doStop();
			synchronized(state) {
				state.notifyAll();
			}
		} else {
			throw new IllegalStateException(
				"Not allowed to stop while state is \"" + stateRef.get() + "\""
			);
		}
		return this;
	}

	@Override
	public final AsyncRunnableBase await()
	throws IllegalStateException, InterruptedException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
		return this;
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws IllegalStateException, InterruptedException {
		long t, timeOutMilliSec = timeUnit.toMillis(timeout);
		t = System.currentTimeMillis();
		while(System.currentTimeMillis() - t < timeOutMilliSec) {
			if(!isStarted()) {
				return true;
			}
			synchronized(state) {
				state.wait(100);
			}
		}
		return false;
	}

	@Override
	public final void close()
	throws IllegalStateException, IOException {
		// stop first
		if(stateRef.compareAndSet(STARTED, STOPPED)) {
			doStop();
		}
		// then close actually
		doClose();
		stateRef.set(null);
		synchronized(state) {
			state.notifyAll();
		}
	}

	protected final void doFinish() {
		stateRef.set(FINISHED);
		synchronized(state) {
			state.notifyAll();
		}
	}

	protected abstract void doStart();

	protected abstract void doStop()
	throws RemoteException;

	protected abstract void doClose()
	throws IOException;
}
