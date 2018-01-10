package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.scenario.sna.AsyncRunnable.State.FINISHED;
import static com.emc.mongoose.scenario.sna.AsyncRunnable.State.INITIAL;
import static com.emc.mongoose.scenario.sna.AsyncRunnable.State.STARTED;
import static com.emc.mongoose.scenario.sna.AsyncRunnable.State.STOPPED;

import org.apache.logging.log4j.Level;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AsyncRunnableBase
implements AsyncRunnable {

	private AtomicReference<State> stateRef = new AtomicReference<>(INITIAL);
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
	throws IllegalStateException {
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
	public final boolean await(final long timeout, final TimeUnit timeUnit)
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
	throws IllegalStateException {
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
				LogUtil.exception(Level.ERROR, e, "Failed to await \"{}\"", toString());
			} catch(final InterruptedException e) {
				throw new CancellationException();
			}
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.ERROR, e, "Failed to start \"{}\"", toString());
		} finally {
			try {
				close();
			} catch(final IllegalStateException e) {
				LogUtil.exception(Level.ERROR, e, "Failed to close \"{}\"", toString());
			}
		}
	}

	protected final void doFinish() {
		stateRef.set(FINISHED);
		state.notifyAll();
	}

	protected abstract void doStart();

	protected abstract void doStop();

	protected abstract void doClose();
}
