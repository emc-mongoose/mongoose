package com.emc.mongoose.api.common.supply.async;

import com.emc.mongoose.api.common.concurrent.InitCallable;
import com.emc.mongoose.api.common.concurrent.InitRunnable;
import com.emc.mongoose.api.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.api.common.supply.BasicUpdatingValueSupplier;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 10.02.16.
 */
public class AsyncUpdatingValueSupplier<T>
extends BasicUpdatingValueSupplier<T> {
	
	private static final int MAX_UPDATE_TASKS = 1024;
	private static final BlockingQueue<InitRunnable>
		UPDATE_TASKS = new ArrayBlockingQueue<>(MAX_UPDATE_TASKS);
	private static final Thread UPDATE_VALUES_WORKER = new Thread() {
		
		{
			setName("asyncUpdateValuesWorker");
			setDaemon(true);
			start();
		}
		
		@Override
		public final void run() {
			InitRunnable nextUpdateTask;
			while(!UPDATE_VALUES_WORKER.isInterrupted()) {
				try {
					nextUpdateTask = UPDATE_TASKS.take();
					try {
						if(nextUpdateTask.isInitialized()) {
							nextUpdateTask.run();
						}
						if(!UPDATE_TASKS.offer(nextUpdateTask)) {
							throw new OmgDoesNotPerformException(
								"Failed to add the new update task"
							);
						}
					} catch(final Throwable throwable) {
						throwable.printStackTrace(System.err);
					}
					LockSupport.parkNanos(1_000_000);
				} catch(final InterruptedException e) {
					break;
				}
			}
		}
	};
	
	private final InitRunnable updateTask;
	
	public AsyncUpdatingValueSupplier(final T initialValue, final InitCallable<T> updateAction)
	throws OmgDoesNotPerformException {
		super(initialValue, null);
		if(updateAction == null) {
			throw new NullPointerException("Argument should not be null");
		}
		updateTask = new InitRunnable() {
			@Override
			public final boolean isInitialized() {
				return updateAction.isInitialized();
			}
			@Override
			public final void run() {
				try {
					lastValue = updateAction.call();
				} catch (final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		};
		if(!UPDATE_TASKS.offer(updateTask)) {
			throw new OmgDoesNotPerformException(
				"Failed to register the update task \"{" + updateTask + "}\" for the generator \"{"
				+ this + "}\", possibly there are too many update tasks registered"
			);
		}
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
	
	/*@Override
	public final int get(final List<T> buffer, final int limit) {
		int count = 0;
		for(; count < limit; count ++) {
			buffer.add(lastValue);
		}
		return count;
	}*/
	
	@Override
	public long skip(final long count) {
		return 0;
	}
	
	@Override
	public void close()
	throws IOException {
		super.close();
		UPDATE_TASKS.remove(updateTask);
	}
}
