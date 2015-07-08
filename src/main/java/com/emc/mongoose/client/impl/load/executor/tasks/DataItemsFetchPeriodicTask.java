package com.emc.mongoose.client.impl.load.executor.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
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
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
/**
 Created by kurila on 17.12.14.
 */
public final class DataItemsFetchPeriodicTask<T extends DataItem>
implements PeriodicTask<Collection<T>> {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private final LoadSvc<T> loadSvc;
	private final AtomicReference<Collection<T>> result = new AtomicReference<>();
	//
	public
	DataItemsFetchPeriodicTask(final LoadSvc<T> loadSvc) {
		this.loadSvc = loadSvc;
	}
	//
	@Override
	public final void run() {
		try {
			final Collection<T> nextFrame = loadSvc.takeFrame();
			if(nextFrame != null) {
				result.set(nextFrame);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Got frame containing {} items", nextFrame.size());
				}
			}
		} catch(final RemoteException | InterruptedException e) {
			//  ignored
			//LogUtil.exception(LOG, Level.WARN, e, "Failed to fetch the frame");
		}
	}
	//
	@Override
	public final Collection<T> getLastResult() {
		return result.getAndSet(null);
	}
}
