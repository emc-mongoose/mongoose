package com.emc.mongoose.client.impl.load.executor.tasks;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.log.LogUtil;
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
public final class SubmitToLoadSvcTask<T extends DataItem>
implements Runnable, Reusable<SubmitToLoadSvcTask> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static InstancePool<SubmitToLoadSvcTask>
		INSTANCE_POOL = new InstancePool<>(SubmitToLoadSvcTask.class);
	//
	@SuppressWarnings("unchecked")
	public static <T extends DataItem> SubmitToLoadSvcTask<T> getInstance(
		final LoadSvc<T> loadSvc, final T dataItem
	) {
		return INSTANCE_POOL.take(loadSvc, dataItem);
	}
	//
	private LoadSvc<T> loadSvc = null;
	private T dataItem = null;
	//
	@Override @SuppressWarnings("unchecked")
	public final SubmitToLoadSvcTask reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				loadSvc = (LoadSvc<T>) args[0];
			}
			if(args.length > 1) {
				dataItem = (T) args[1];
			}
		}
		return this;
	}
	//
	@Override
	public final void release() {
		INSTANCE_POOL.release(this);
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
