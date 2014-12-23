package com.emc.mongoose.base.data.persist;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.Markers;
//
import com.emc.mongoose.util.pool.InstancePool;
import com.emc.mongoose.util.pool.Reusable;
import com.emc.mongoose.util.threading.WorkerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
			new LinkedBlockingQueue<Runnable>(), new WorkerFactory("dataItemLogWorker")
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
			LOG.info(Markers.DATA_LIST, dataItem.toString());
		}
		//
		@Override
		public final void close() {
			TASK_POOL.release(this);
		}
		//
		@Override @SuppressWarnings("unchecked")
		public final DataItemLogTask<T> reuse(final Object... args) {
			this.dataItem = (T) args[0];
			return this;
		}
	}
	//
	@Override
	public void submit(final T data) {
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
				Main.RUN_TIME_CONFIG.get().getRunReqTimeOutMilliSec(), TimeUnit.MILLISECONDS
			);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted while waiting the remaining tasks to complete");
		} finally {
			LOG.debug(Markers.MSG, "Dropped {} tasks", shutdownNow().size());
		}
		LOG.trace(Markers.MSG, "invoking close() here does nothing");
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
