package com.emc.mongoose.base.load.client.impl.tasks;
//
import com.emc.mongoose.base.load.client.LoadClient;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.threading.PeriodicTask;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 17.12.14.
 */
public class CountLimitWaitTask
implements PeriodicTask<Long> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadClient loadClient;
	private final PeriodicTask<Long> getValueTasks[];
	private final long maxCount;
	private final AtomicLong processedCount = new AtomicLong(0);
	//
	public CountLimitWaitTask(
		final LoadClient loadClient, final long maxCount, final PeriodicTask<Long> getValueTasks[]
	) {
		this.loadClient = loadClient;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.getValueTasks = getValueTasks;
	}
	//
	@Override
	public final void run() {
		for(final PeriodicTask<Long> nextCountTask : getValueTasks) {
			if(maxCount <= processedCount.addAndGet(nextCountTask.getLastResult())) {
				try {
					loadClient.shutdown();
				} catch(final RemoteException e) {
					TraceLogger.failure(LOG, Level.WARN, e, "Failed to shutdown the load client");
				}
			}
		}
	}
	//
	@Override
	public final Long getLastResult() {
		return processedCount.get();
	}
}
