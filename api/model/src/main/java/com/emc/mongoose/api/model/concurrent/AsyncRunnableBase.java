package com.emc.mongoose.api.model.concurrent;

import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.FINISHED;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.INITIAL;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.SHUTDOWN;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.STARTED;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.STOPPED;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AsyncRunnableBase
implements AsyncRunnable {

	private final AtomicReference<State> stateRef = new AtomicReference<>(INITIAL);
	private final Lock stateLock = new ReentrantLock();
	private final Condition stateChangeCond = stateLock.newCondition();

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
	public boolean isShutdown() {
		return SHUTDOWN.equals(stateRef.get());
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
	public boolean isClosed() {
		return null == stateRef.get();
	}

	@Override
	public final AsyncRunnableBase start()
	throws IllegalStateException {
		if(stateRef.compareAndSet(INITIAL, STARTED) || stateRef.compareAndSet(STOPPED, STARTED)) {
			stateLock.lock();
			try {
				doStart();
				stateChangeCond.signalAll();
			} finally {
				stateLock.unlock();
			}
		} else {
			throw new IllegalStateException(
				"Not allowed to start while state is \"" + stateRef.get() + "\""
			);
		}
		return this;
	}

	@Override
	public final AsyncRunnableBase shutdown()
	throws IllegalStateException {
		if(stateRef.compareAndSet(STARTED, SHUTDOWN)) {
			stateLock.lock();
			try {
				doShutdown();
				stateChangeCond.signalAll();
			} finally {
				stateLock.unlock();
			}
		} else {
			throw new IllegalStateException(
				"Not allowed to shutdown while state is \"" + stateRef.get() + "\""
			);
		}
		return this;
	}

	@Override
	public final AsyncRunnableBase stop()
	throws IllegalStateException, RemoteException {
		try {
			shutdown();
		} catch(final IllegalStateException ignored) {
		}
		if(stateRef.compareAndSet(STARTED, STOPPED) || stateRef.compareAndSet(SHUTDOWN, STOPPED)) {
			stateLock.lock();
			try {
				doStop();
				stateChangeCond.signalAll();
			} finally {
				stateLock.unlock();
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
		stateLock.lock();
		try {
			return stateChangeCond.await(timeout, timeUnit);
		} finally {
			stateLock.unlock();
		}
	}

	@Override
	public void close()
	throws IllegalStateException, IOException {
		// stop first
		try {
			stop();
		} catch(final IllegalStateException ignored) {
		}
		// then close actually
		stateLock.lock();
		try {
			doClose();
			stateRef.set(null);
			stateChangeCond.signalAll();
		} finally {
			stateLock.unlock();
		}
	}

	protected abstract void doStart();

	protected abstract void doShutdown();

	protected abstract void doStop()
	throws RemoteException;

	protected abstract void doClose()
	throws IOException;
}
