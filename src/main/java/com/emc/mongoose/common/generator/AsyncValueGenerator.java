package com.emc.mongoose.common.generator;
//
import com.emc.mongoose.common.log.LogUtil;
//
import org.apache.logging.log4j.Level;
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
	protected interface InitRunnable extends Initializable, Runnable {}
	private final static List<InitRunnable> UPDATE_TASKS = new ArrayList<>();
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
				synchronized (UPDATE_TASKS) {
					for (final InitRunnable updTask : UPDATE_TASKS) {
						if(updTask.isInitialized()) {
							updTask.run();
						}
						LockSupport.parkNanos(1);
					}
					Thread.yield();
				}
			}
		}
	};
	//
	protected interface InitCallable<T> extends Initializable, Callable<T> {}
	//
	public AsyncValueGenerator(final T initialValue, final InitCallable<T> updateAction) {
		super(initialValue, null);
		synchronized (UPDATE_TASKS) {
			UPDATE_TASKS.add(
				new InitRunnable() {
					@Override
					public final boolean isInitialized() {
						return updateAction.isInitialized();
					}
					@Override
					public final void run() {
						try {
							lastValue = updateAction.call();
						} catch (final Exception e) {
							LogUtil.exception(
								LOG, Level.WARN, e,
								"Failed to execute the update action \"{}\"", updateAction
							);
						}
					}
				}
			);
		}
	}

	//
	@Override
	public final T get() {
		// do not refresh on the request
		while(lastValue == null) {
			LockSupport.parkNanos(1);
			Thread.yield();
		}
		return lastValue;
	}
}
