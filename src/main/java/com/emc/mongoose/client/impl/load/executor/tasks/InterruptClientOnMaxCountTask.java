package com.emc.mongoose.client.impl.load.executor.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.api.load.executor.tasks.PeriodicTask;
//
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
/**
 Created by kurila on 17.12.14.
 */
public class InterruptClientOnMaxCountTask
implements PeriodicTask<Long> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadClient loadClient;
	private final IOStats ioStats;
	private final long maxCount;
	//
	public InterruptClientOnMaxCountTask(
		final LoadClient loadClient, final long maxCount, final IOStats ioStats
	) {
		this.loadClient = loadClient;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		this.ioStats = ioStats;
	}
	//
	@Override
	public final void run() {
		if(maxCount >= getLastResult()) {
			try {
				loadClient.interrupt();
				LOG.debug(
					Markers.MSG, "Load client \"{}\" was interrupted due to count limit {}",
					loadClient, maxCount
				);
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to shutdown the load client");
			}
		}
	}
	//
	@Override
	public final Long getLastResult() {
		final IOStats.Snapshot snapshot = ioStats.getSnapshot();
		return snapshot.getSuccCount() + snapshot.getFailCount();
	}
}
