package com.emc.mongoose.common.io.value.async;
//
import com.emc.mongoose.common.io.value.BasicValueInput;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 10.02.16.
 */
public class AsyncValueInput<T>
extends BasicValueInput<T> {
	//
	private static final Logger LOG = LogManager.getLogger();
	private static final int MAX_UPDATE_TASKS = 1024;
	private static final BlockingQueue<InitRunnable>
		UPDATE_TASKS = new ArrayBlockingQueue<>(MAX_UPDATE_TASKS);
	private static final Thread UPDATE_VALUES_WORKER = new Thread() {
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
					LockSupport.parkNanos(1_000_000);
				} catch(final InterruptedException e) {
					break;
				}
			}
		}
	};
	//
	private final InitRunnable updateTask;
	//
	public AsyncValueInput(final T initialValue, final InitCallable<T> updateAction) {
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
	public static abstract class InitializedCallableBase<T>
	implements InitCallable<T> {
		@Override
		public final boolean isInitialized() {
			return true;
		}
	}
	//
	@Override
	public final T get() {
		// do not refresh on the request
		return lastValue;
	}
	//
	@Override
	public final int get(final List<T> buffer, final int limit) {
		int count = 0;
		for(; count < limit; count ++) {
			buffer.add(lastValue);
		}
		return count;
	}
	//
	@Override
	public void skip(final long count)
	throws IOException {
	}
	//
	@Override
	public void close()
	throws IOException {
		super.close();
		UPDATE_TASKS.remove(updateTask);
	}
}
