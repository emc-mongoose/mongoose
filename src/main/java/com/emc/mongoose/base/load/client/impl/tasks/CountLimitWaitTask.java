package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.emc.mongoose.base.load.client.LoadClient;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 17.12.14.
 */
public class CountLimitWaitTask
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadClient loadClient;
	private final ExecutorService mgmtConnExecutor;
	private final long maxCount;
	private final GaugeValueTask<Long> getValueTasks[];
	//
	public CountLimitWaitTask(
		final LoadClient loadClient, final ExecutorService mgmtConnExecutor,
		final long maxCount, final GaugeValueTask<Long> getValueTasks[]
	) {
		this.loadClient = loadClient;
		this.mgmtConnExecutor = mgmtConnExecutor;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.getValueTasks = getValueTasks;
	}
	//
	@Override
	public final void run() {
		int i, tasksCount = getValueTasks.length;
		long processedCount = 0;
		final Future<Long> futureValues[] = new Future[tasksCount];
		do {
			try {
				for(i = 0; i < tasksCount; i ++) {
					futureValues[i] = mgmtConnExecutor.submit(getValueTasks[i]);
				}
				Thread.yield();
				for(final Future<Long> futureValue : futureValues) {
					try {
						processedCount += futureValue.get();
					} catch(final InterruptedException e) {
						LOG.debug(Markers.MSG, "Interrupted");
						return; // break will cause the recursion
					} catch(final ExecutionException e) {
						TraceLogger.failure(LOG, Level.DEBUG, e, "Failed to get metric value");
					}
				}
			} catch(final RejectedExecutionException e) {
				TraceLogger.failure(LOG, Level.DEBUG, e, "Failed to submit the task");
				Thread.yield();
			}
		} while(maxCount > processedCount);
		//
		try {
			loadClient.shutdown();
		} catch(final RemoteException e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to shut down the load client");
		}
	}
}
