package com.emc.mongoose.common.generator;
//
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 10.02.16.
 */
public class AsyncValueGenerator<T>
extends BasicValueGenerator<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int MAX_UPDATE_TASKS = 1024;
	protected interface InitRunnable extends Initializable, Runnable {}
	private final static BlockingQueue<InitRunnable>
		UPDATE_TASKS = new ArrayBlockingQueue<>(MAX_UPDATE_TASKS);
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
			InitRunnable nextUpdateTask;
			while(!UPDATE_VALUES_WORKER.isInterrupted()) {
				try {
					nextUpdateTask = UPDATE_TASKS.take();
					try {
						if(nextUpdateTask.isInitialized()) {
							nextUpdateTask.run();
						}
						if(!UPDATE_TASKS.offer(nextUpdateTask)) {
							LOG.warn(
								Markers.ERR,
								"Failed to put the update task \"{}\" back into the queue, " +
								"possibly there are too many update tasks registered"
							);
						}
					} catch(final Throwable throwable) {
						LogUtil.exception(
							LOG, Level.ERROR, throwable, "The update task \"{}\" has thrown an " +
							"exception, unregistering it"
						);
					}
					Thread.yield();
					TimeUnit.MICROSECONDS.sleep(1);
				} catch(final InterruptedException e) {
					break;
				}
			}
		}
	};
	//
	protected interface InitCallable<T> extends Initializable, Callable<T> {}
	//
	public AsyncValueGenerator(final T initialValue, final InitCallable<T> updateAction) {
		super(initialValue, null);
		final InitRunnable updateTask = new InitRunnable() {
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
		};
		if(UPDATE_TASKS.offer(updateTask)) {
			LOG.debug(
				Markers.MSG, "Registered update task \"{}\" for the generator \"{}\"",
				updateTask, this
			);
		} else {
			LOG.warn(
				Markers.ERR,
				"Failed to register the update task \"{}\" for the generator \"{}\", " +
				"possibly there are too many update tasks registered", updateTask, this
			);
		}
	}

	//
	@Override
	public final T get() {
		// do not refresh on the request
		return lastValue;
	}
}
