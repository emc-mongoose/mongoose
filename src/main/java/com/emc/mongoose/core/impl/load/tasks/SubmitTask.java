package com.emc.mongoose.core.impl.load.tasks;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
/**
 Created by andrey on 22.05.15.
 */
public final class SubmitTask<T extends DataItem>
implements Runnable, Reusable<SubmitTask> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static InstancePool<SubmitTask>
		INSTANCE_POOL = new InstancePool<>(SubmitTask.class);
	//
	public static <T extends DataItem>  SubmitTask getInstance(
		final LoadExecutor<T> loadExecutor, final T dataItem
	) {
		return INSTANCE_POOL.take(loadExecutor, dataItem);
	}
	//
	private LoadExecutor<T> loadExecutor = null;
	private T dataItem = null;
	//
	@Override @SuppressWarnings("unchecked")
	public final SubmitTask reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				loadExecutor = (LoadExecutor<T>) args[0];
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
			loadExecutor.submitSync(dataItem);
		} catch(final RemoteException e){
			LogUtil.exception(LOG, Level.WARN, e, "Failed to submit the data item {}", dataItem);
		} finally {
			release();
		}
	}
}
