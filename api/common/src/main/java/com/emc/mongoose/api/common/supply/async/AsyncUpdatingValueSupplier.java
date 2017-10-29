package com.emc.mongoose.api.common.supply.async;

import com.github.akurilov.commons.concurrent.InitCallable;

import com.github.akurilov.coroutines.Coroutine;
import com.github.akurilov.coroutines.CoroutinesProcessor;
import com.github.akurilov.coroutines.ExclusiveCoroutineBase;

import com.emc.mongoose.api.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.api.common.supply.BasicUpdatingValueSupplier;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 Created by kurila on 10.02.16.
 */
public class AsyncUpdatingValueSupplier<T>
extends BasicUpdatingValueSupplier<T> {
	
	private static final Logger LOG = Logger.getLogger(AsyncUpdatingValueSupplier.class.getName());
	
	private final Coroutine updateTask;
	
	public AsyncUpdatingValueSupplier(
		final CoroutinesProcessor coroutinesProcessor, final T initialValue,
		final InitCallable<T> updateAction
	)
	throws OmgDoesNotPerformException {

		super(initialValue, null);
		if(updateAction == null) {
			throw new NullPointerException("Argument should not be null");
		}

		updateTask = new ExclusiveCoroutineBase(coroutinesProcessor) {

			@Override
			protected final void invokeTimedExclusively(final long startTimeNanos) {
				try {
					lastValue = updateAction.call();
				} catch(final Exception e) {
					LOG.log(Level.WARNING, "Failed to execute the value update action", e);
					e.printStackTrace(System.err);
				}
			}

			@Override
			protected final void doClose()
			throws IOException {
				lastValue = null;
			}
		};

		updateTask.start();
	}
	
	public static abstract class InitializedCallableBase<T>
	implements InitCallable<T> {
		@Override
		public final boolean isInitialized() {
			return true;
		}
	}
	
	@Override
	public final T get() {
		// do not refresh on the request
		return lastValue;
	}
	
	@Override
	public final int get(final List<T> buffer, final int limit) {
		int count = 0;
		for(; count < limit; count ++) {
			buffer.add(lastValue);
		}
		return count;
	}
	
	@Override
	public long skip(final long count) {
		return 0;
	}
	
	@Override
	public void close()
	throws IOException {
		super.close();
		updateTask.close();
	}
}
