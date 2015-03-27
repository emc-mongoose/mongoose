package com.emc.mongoose.client.impl.load.executor.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.logging.TraceLogger;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
/**
 Created by kurila on 17.12.14.
 */
public final class FrameFetchPeriodicTask<T extends List<U>, U extends DataItem>
implements PeriodicTask<T> {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private final LoadSvc<U> loadSvc;
	private final AtomicReference<T> result = new AtomicReference<>();
	//
	public FrameFetchPeriodicTask(final LoadSvc<U> loadSvc) {
		this.loadSvc = loadSvc;
	}
	//
	@Override
	public final void run() {
		try {
			result.set((T) loadSvc.takeFrame());
		} catch(final RemoteException e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to fetch the frame");
		}
	}
	//
	@Override
	public final T getLastResult() {
		return result.getAndSet(null);
	}
}
