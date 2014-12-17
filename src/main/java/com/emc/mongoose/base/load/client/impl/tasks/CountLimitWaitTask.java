package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
/**
 Created by kurila on 17.12.14.
 */
public class CountLimitWaitTask
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final long maxCount;
	private final ExecutorService mgmtConnExecutor;
	private final GaugeValueTask<Long> getValueTasks[];
	//
	@SuppressWarnings("unchecked")
	public CountLimitWaitTask(
		final long maxCount, final ExecutorService mgmtConnExecutor,
		final GaugeValueTask getValueTasks[]
	)
		throws ClassCastException {
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.mgmtConnExecutor = mgmtConnExecutor;
		this.getValueTasks = (GaugeValueTask<Long>[]) getValueTasks;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void run() {
		int i, tasksCount = getValueTasks.length;
		long processedCount = 0;
		final Future<Long> futureValues[] = new Future[tasksCount];
		do {
			for(i = 0; i < tasksCount; i ++) {
				futureValues[i] = mgmtConnExecutor.submit(getValueTasks[i]);
			}
			for(final Future<Long> futureValue: futureValues) {
				try {
					processedCount += futureValue.get();
				} catch(final InterruptedException e) {
					LOG.debug(Markers.MSG, "Interrupted");
					break;
				} catch(final ExecutionException e) {
					ExceptionHandler.trace(LOG, Level.DEBUG, e, "Failed to get metric value");
				}
			}
		} while(maxCount > processedCount);
	}
}
