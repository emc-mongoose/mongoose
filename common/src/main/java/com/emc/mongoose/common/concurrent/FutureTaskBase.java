package com.emc.mongoose.common.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 Created on 20.07.16.
 */
@SuppressWarnings("Duplicates")
public abstract class FutureTaskBase<V> implements RunnableFuture<V> {

	private AtomicBoolean completed = new AtomicBoolean(false);
	private volatile V result;
	private volatile Throwable throwable;

	@Override
	public boolean isDone() {
		return this.completed.get();
	}

	private V getResult() throws ExecutionException {
		if (throwable != null) {
			throw new ExecutionException(throwable);
		}
		return result;
	}

	@Override
	public V get()
	throws InterruptedException, ExecutionException {
		while(!completed.get()) {
			wait();
		}
		return getResult();
	}

	@Override
	public V get(long timeout, final TimeUnit unit)
	throws InterruptedException, ExecutionException, TimeoutException {
		final long timeoutInMillis;
		if (timeout < 0 || unit == null) {
			throw new IllegalArgumentException();
		} else if (timeout == 0) {
			timeoutInMillis = Long.MAX_VALUE;
		} else {
			timeoutInMillis = unit.toMillis(timeout);
		}
		final long startTime = System.currentTimeMillis();
		long waitTime = timeoutInMillis;
		if (completed.get()) {
			return getResult();
		} else if (waitTime <= 0) {
			throw new TimeoutException();
		} else {
			while(true) {
				wait(waitTime);
				if (completed.get()) {
					return getResult();
				} else {
					waitTime = timeoutInMillis - (System.currentTimeMillis() - startTime);
					if (waitTime <= 0) {
						throw new TimeoutException();
					}
				}
			}
		}
	}

	protected boolean set(final V v) {
		if (completed.get()) {
			return false;
		}
		completed.set(true);
		result = v;
		notifyAll();
		return true;
	}

	protected boolean setException(final Throwable t) {
		if (completed.get()) {
			return false;
		}
		completed.set(true);
		this.throwable = t;
		notifyAll();
		return true;
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCancelled() {
		return false;
	}
}
