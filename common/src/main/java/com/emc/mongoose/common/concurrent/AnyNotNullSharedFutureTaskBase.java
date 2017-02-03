package com.emc.mongoose.common.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created by kurila on 15.09.16.
 */
public abstract class AnyNotNullSharedFutureTaskBase<V>
implements RunnableFuture<V> {
	
	private final CountDownLatch sharedLatch;
	private final AtomicBoolean completed;
	
	private final AtomicReference<V> resultRef;
	private volatile Throwable cause;
	
	protected AnyNotNullSharedFutureTaskBase(
		final AtomicReference<V> resultRef, final CountDownLatch sharedLatch
	) {
		this.resultRef = resultRef;
		this.sharedLatch = sharedLatch;
		completed = new AtomicBoolean(false);
	}
	
	@Override
	public final boolean isDone() {
		return this.completed.get();
	}
	
	private V getResult()
	throws ExecutionException {
		if(cause != null) {
			throw new ExecutionException(cause);
		}
		return resultRef.get();
	}
	
	@Override
	public final V get()
	throws InterruptedException, ExecutionException {
		sharedLatch.await();
		return resultRef.get();
	}
	
	@Override
	public final V get(long timeout, final TimeUnit unit)
	throws InterruptedException, ExecutionException, TimeoutException {
		final long timeoutInMillis;
		if(timeout < 0) {
			throw new IllegalArgumentException();
		} else if(timeout == 0) {
			timeoutInMillis = Long.MAX_VALUE;
		} else {
			timeoutInMillis = unit.toMillis(timeout);
		}
		final long startTime = System.currentTimeMillis();
		long waitTime = timeoutInMillis;
		if(completed.get()) {
			return getResult();
		} else if(waitTime <= 0) {
			throw new TimeoutException();
		} else {
			while(true) {
				sharedLatch.await(waitTime, TimeUnit.MILLISECONDS);
				if(completed.get()) {
					return getResult();
				} else {
					waitTime = timeoutInMillis - (System.currentTimeMillis() - startTime);
					if(waitTime <= 0) {
						throw new TimeoutException();
					}
				}
			}
		}
	}
	
	protected boolean set(final V v) {
		if(!completed.get() && completed.compareAndSet(false, true)) {
			resultRef.compareAndSet(null, v);
			sharedLatch.countDown();
			return true;
		}
		return false;
	}
	
	protected boolean setException(final Throwable cause) {
		if(completed.compareAndSet(false, true)) {
			this.cause = cause;
			sharedLatch.countDown();
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
