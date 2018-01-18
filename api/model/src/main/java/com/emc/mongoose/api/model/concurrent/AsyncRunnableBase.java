package com.emc.mongoose.api.model.concurrent;

import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.FINISHED;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.INITIAL;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.STARTED;
import static com.emc.mongoose.api.model.concurrent.AsyncRunnable.State.STOPPED;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public abstract class AsyncRunnableBase
implements AsyncRunnable {

	private static final Logger LOG = Logger.getLogger(AsyncRunnableBase.class.getName());

	private final AtomicReference<State> stateRef = new AtomicReference<>(INITIAL);
	protected final Object state = new Object();

	@Override
	public final State state() {
		return stateRef.get();
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
			state.notifyAll();
		} else {
			throw new IllegalStateException(
				"Not allowed to start while state is \"" + stateRef.get() + "\""
			);
		}
		return this;
	}

	@Override
	public final AsyncRunnableBase stop()
	throws IllegalStateException, Exception {
		if(stateRef.compareAndSet(STARTED, STOPPED)) {
			doStop();
			state.notifyAll();
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
			if(!STARTED.equals(stateRef.get())) {
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
	throws IllegalStateException, Exception {
		// stop first
		if(stateRef.compareAndSet(STARTED, STOPPED)) {
			doStop();
		}
		// then close actually
		doClose();
		stateRef.set(null);
		state.notifyAll();
	}

	@Override
	public final void run() {
		try {
			start();
			try {
				await();
			} catch(final IllegalStateException e) {
				LOG.warning("Failed to await \"" + toString() + "\"");
			} catch(final InterruptedException e) {
				throw new CancellationException();
			}
		} catch(final IllegalStateException e) {
			LOG.warning("Failed to start \"" + toString() + "\"");
		} finally {
			try {
				close();
			} catch(final Exception e) {
				LOG.warning("Failed to close \"" + toString() + "\"");
			}
		}
	}

	protected final void doFinish() {
		stateRef.set(FINISHED);
		state.notifyAll();
	}

	protected abstract void doStart();

	protected abstract void doStop()
	throws Exception;

	protected abstract void doClose()
	throws Exception;
}
