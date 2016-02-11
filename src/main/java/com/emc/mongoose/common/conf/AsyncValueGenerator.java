package com.emc.mongoose.common.conf;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 10.02.16.
 */
public class AsyncValueGenerator<T>
extends BasicValueGenerator<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static List<Runnable> UPDATE_TASKS = new ArrayList<>();
	private final static Thread UPDATE_VALUES_WORKER = new Thread() {
		//
		{
			setName("asyncUpdateValuesWorker");
			setDaemon(true);
			start();
		}
		//
		@Override
		public final void run() {
			while(!UPDATE_VALUES_WORKER.isInterrupted()) {
				for(final Runnable updTask : UPDATE_TASKS) {
					LockSupport.parkNanos(1);
					updTask.run();
				}
				Thread.yield();
			}
		}
	};
	//
	public AsyncValueGenerator(final T initialValue, final Callable<T> updateTask) {
		super(initialValue, null);
		UPDATE_TASKS.add(
			new Runnable() {
				@Override
				public final void run() {
					lastValue = AsyncValueGenerator.super.get();
				}
			}
		);
	}
	//
	@Override
	public final T get() {
		// do not refresh on the request
		return lastValue;
	}
}
