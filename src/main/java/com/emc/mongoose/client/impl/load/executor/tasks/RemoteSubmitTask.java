package com.emc.mongoose.client.impl.load.executor.tasks;
//
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
/**
 Created by andrey on 22.05.15.
 */
public final class RemoteSubmitTask<T extends DataItem>
implements Runnable, Reusable<RemoteSubmitTask> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadSvc<T> loadSvc;
	//
	public RemoteSubmitTask(final LoadSvc<T> loadSvc) {
		this.loadSvc = loadSvc;
	}
	private T dataItem = null;
	//
	@Override @SuppressWarnings("unchecked")
	public final RemoteSubmitTask reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(args != null && args.length > 0) {
			dataItem = (T) args[0];
		}
		return this;
	}
	//
	@Override
	public final void release() {
	}
	//
	@Override
	public final
	void run() {
		try {
			loadSvc.submit(dataItem);
		} catch(final InterruptedException | RemoteException e){
			LogUtil.exception(LOG, Level.WARN, e, "Failed to submit the data item {}", dataItem);
		} finally {
			release();
		}
	}
}
