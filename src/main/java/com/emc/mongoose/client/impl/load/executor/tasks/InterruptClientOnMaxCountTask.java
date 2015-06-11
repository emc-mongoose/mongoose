package com.emc.mongoose.client.impl.load.executor.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.api.load.executor.tasks.PeriodicTask;
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
public class InterruptClientOnMaxCountTask
implements PeriodicTask<Long> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadClient loadClient;
	private final PeriodicTask<Long> getValueTasks[];
	private final long maxCount;
	private final AtomicLong processedCount = new AtomicLong(0);
	//
	public InterruptClientOnMaxCountTask(
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
			if(nextCountTask.getLastResult() != null && maxCount <= processedCount.addAndGet(nextCountTask.getLastResult())) {
				try {
					loadClient.interrupt();
					LOG.debug(
						LogUtil.MSG, "Load client \"{}\" was interrupted due to count limit {}",
						loadClient, maxCount
					);
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to shutdown the load client");
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
