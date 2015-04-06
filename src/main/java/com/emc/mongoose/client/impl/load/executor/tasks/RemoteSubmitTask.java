package com.emc.mongoose.client.impl.load.executor.tasks;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 18.12.14.
 */
public class RemoteSubmitTask<T extends DataItem>
implements Runnable, Reusable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private LoadSvc<T> loadSvc = null;
	private T dataItem = null;
	//
	private final RunTimeConfig thrLocalConf = RunTimeConfig.getContext();
	private final int
		retryMaxCount = thrLocalConf.getRunRetryCountMax(),
		retryDelayMilliSec = thrLocalConf.getRunRetryDelayMilliSec();
	//
	private final static InstancePool<RemoteSubmitTask>
		INSTANCE_POOL = new InstancePool<>(RemoteSubmitTask.class);
	//
	@SuppressWarnings("unchecked")
	public static <U extends DataItem> RemoteSubmitTask<U> getInstanceFor(
		final LoadSvc<U> loadSvc, final U dataItem
	) throws InterruptedException {
		return INSTANCE_POOL.take(loadSvc, dataItem);
	}
	//
	@Override
	public final void run() {
		int rejectCount = 0;
		try {
			do {
				try {
					try {
						loadSvc.submit(dataItem);
						break;
					} catch(final NoSuchObjectException | ServerException e) {
						LOG.debug(LogUtil.ERR, "Load service \"{}\" seems to be shut down already");
						break;
					} catch(final RemoteException e) {
						rejectCount ++;
						Thread.sleep(retryDelayMilliSec);
					}
				} catch(final InterruptedException e) {
					LOG.debug(LogUtil.MSG, "Interrupted");
					break;
				}
			} while(rejectCount > retryMaxCount);
		} finally {
			release();
		}
	}
	//
	private final AtomicBoolean isAvailable = new AtomicBoolean(true);
	//
	@Override
	public final void release() {
		if(isAvailable.compareAndSet(false, true)) {
			INSTANCE_POOL.release(this);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final RemoteSubmitTask<T> reuse(final Object... args) {
		if(isAvailable.compareAndSet(true, false)) {
			if(args.length > 0) {
				loadSvc = (LoadSvc<T>) args[0];
			}
			if(args.length > 1) {
				dataItem = (T) args[1];
			}
		} else {
			throw new IllegalStateException("Not yet released instance reuse attempt");
		}
		return this;
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int compareTo(Reusable another) {
		return another == null ? 1 : hashCode() - another.hashCode();
	}
}
