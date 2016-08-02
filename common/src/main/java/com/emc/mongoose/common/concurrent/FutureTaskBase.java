package com.emc.mongoose.common.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 Created on 20.07.16.
 */
@SuppressWarnings("Duplicates")
public abstract class FutureTaskBase<V>
implements RunnableFuture<V> {

	private final CountDownLatch latch = new CountDownLatch(1);
	private final AtomicBoolean completed = new AtomicBoolean(false);

	private volatile V result;
	private volatile Throwable cause;

	@Override
	public final boolean isDone() {
		return this.completed.get();
	}

	private V getResult()
	throws ExecutionException {
		if(cause != null) {
			throw new ExecutionException(cause);
		}
		return result;
	}

	@Override
	public final V get()
	throws InterruptedException, ExecutionException {
		latch.await();
		return result;
	}

	@Override
	public final V get(long timeout, final TimeUnit unit)
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
				latch.await(waitTime, TimeUnit.MILLISECONDS);
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
		if (!completed.get() && completed.compareAndSet(false, true)) {
			result = v;
			latch.countDown();
			return true;
		}
		return false;
	}

	protected boolean setException(final Throwable cause) {
		if(completed.compareAndSet(false, true)) {
			this.cause = cause;
			latch.countDown();
			return true;
		}
		return false;
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
