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
import java.rmi.ConnectIOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
/**
 Created by kurila on 17.12.14.
 */
public final class DataItemsFetchPeriodicTask<T extends DataItem>
implements PeriodicTask<Collection<T>> {
	//
	private static final Logger LOG = LogManager.getLogger();
	private static final int MAX_TRY_COUNT = 100;
	//
	private final LoadSvc<T> loadSvc;
	private final AtomicReference<Collection<T>> result = new AtomicReference<>();
	//
	public DataItemsFetchPeriodicTask(final LoadSvc<T> loadSvc) {
		this.loadSvc = loadSvc;
	}
	//
	@Override
	public final void run() {
		try {
			Collection<T> nextFrame = null;
			for(int i = 0; i < MAX_TRY_COUNT; i ++) {
				try {
					nextFrame = loadSvc.takeFrame();
					break;
				} catch(final ConnectIOException e) {
					TimeUnit.MILLISECONDS.sleep(i);
				}
			}
			if(nextFrame != null) {
				result.set(nextFrame);
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Got frame containing {} items", nextFrame.size());
				}
			}
		} catch(final NoSuchObjectException ignored) { // if no connection
		} catch(final RemoteException | InterruptedException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to fetch the frame");
		}
	}
	//
	@Override
	public final Collection<T> getLastResult() {
		return result.getAndSet(null);
	}
}
