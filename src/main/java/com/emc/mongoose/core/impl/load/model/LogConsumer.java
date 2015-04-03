package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.pool.InstancePool;
import com.emc.mongoose.common.pool.Reusable;
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 12.05.14.
 A consumer which simply redirects the data items to its logger.
 */
public class LogConsumer<T extends DataItem>
extends ThreadPoolExecutor
implements Consumer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final long maxCount;
	private final AtomicLong count = new AtomicLong(0);
	//
	public LogConsumer(final long maxCount, final int threadCount) {
		super(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(
				maxCount > 0 ?
					Math.min((int)maxCount, RunTimeConfig.getContext().getRunRequestQueueSize()) :
					RunTimeConfig.getContext().getRunRequestQueueSize()
			),
			new NamingWorkerFactory("dataItemLogWorker")
		);
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
	}
	//
	@Override
	public final long getMaxCount() {
		return maxCount;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Pooling the reusable tasks
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<DataItemLogTask> TASK_POOL = new InstancePool<>(
		DataItemLogTask.class
	);
	//
	public final static class DataItemLogTask<T extends DataItem>
	implements Runnable, Reusable {
		//
		public T dataItem = null;
		//
		@Override
		public final void run() {
			try {
				LOG.info(LogUtil.DATA_LIST, dataItem.toString());
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
				TASK_POOL.release(this);
			}
		}
		//
		@Override @SuppressWarnings("unchecked")
		public final DataItemLogTask<T> reuse(final Object... args) {
			if(isAvailable.compareAndSet(true, false)) {
				this.dataItem = (T) args[0];
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
	//
	@Override
	public void submit(final T data)
	throws InterruptedException {
		if(data != null && count.get() < maxCount) {
			super.submit(TASK_POOL.take(data));
			count.incrementAndGet();
		} else {
			shutdown();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void close() {
		if(!isShutdown()) {
			shutdown();
		}
		try {
			awaitTermination(
				RunTimeConfig.getContext().getRunReqTimeOutMilliSec(), TimeUnit.MILLISECONDS
			);
		} catch(final InterruptedException e) {
			LOG.debug(LogUtil.MSG, "Interrupted while waiting the remaining tasks to complete");
		} finally {
			LOG.debug(LogUtil.MSG, "Dropped {} tasks", shutdownNow().size());
		}
		LOG.trace(LogUtil.MSG, "invoking close() here does nothing");
	}
	//
	@Override
	protected final void finalize() {
		close();
		super.finalize();
	}
	//
	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}
}
