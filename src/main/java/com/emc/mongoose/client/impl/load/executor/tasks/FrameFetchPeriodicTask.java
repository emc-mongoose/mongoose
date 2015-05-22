package com.emc.mongoose.client.impl.load.executor.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.tasks.PeriodicTask;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicReference;
/**
 Created by kurila on 17.12.14.
 */
public final class FrameFetchPeriodicTask<T extends DataItem>
implements PeriodicTask<T[]> {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private final LoadSvc<T> loadSvc;
	private final AtomicReference<T[]> result = new AtomicReference<>();
	//
	public FrameFetchPeriodicTask(final LoadSvc<T> loadSvc) {
		this.loadSvc = loadSvc;
	}
	//
	@Override
	public final void run() {
		try {
			final T[] nextFrame = loadSvc.takeFrame();
			if(nextFrame != null) {
				result.set(nextFrame);
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(LogUtil.MSG, "Got frame containing {} items", nextFrame.length);
				}
			}
		} catch(final RemoteException | InterruptedException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to fetch the frame");
		}
	}
	//
	@Override
	public final T[] getLastResult() {
		return result.getAndSet(null);
	}
}
