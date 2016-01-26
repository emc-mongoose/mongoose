package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.io.IOWorker;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.io.task.DirectoryIOTask;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.DirectoryLoadExecutor;
//
import com.emc.mongoose.core.impl.io.task.BasicDirectoryIOTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 23.11.15.
 */
public class BasicDirectoryLoadExecutor<T extends FileItem, C extends Directory<T>>
extends LimitedRateLoadExecutorBase<C>
implements DirectoryLoadExecutor<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final ExecutorService ioTaskExecutor;
	//
	public BasicDirectoryLoadExecutor(
		final AppConfig appConfig,
		final FileIOConfig<T, C> ioConfig, final String[] addrs,
		final int connCountPerNode, final int threadCount, final ItemSrc<C> itemSrc,
		final long maxCount, final int manualTaskSleepMicroSecs, final float rateLimit
	) throws ClassCastException {
		super(
			appConfig, ioConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			manualTaskSleepMicroSecs, rateLimit
		);
		ioTaskExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(maxItemQueueSize), new IOWorker.Factory(getName())
		) {
			@Override @SuppressWarnings("unchecked")
			protected final <V> RunnableFuture<V> newTaskFor(final Runnable task, final V value) {
				return (RunnableFuture<V>) task;
			}
			//
			@Override @SuppressWarnings("unchecked")
			protected final void afterExecute(final Runnable task, final Throwable throwable) {
				final DirectoryIOTask<T, C> ioTask = (DirectoryIOTask<T, C>) task;
				if(throwable == null) {
					ioTaskCompleted(ioTask);
				} else {
					ioTaskFailed(1, throwable);
				}
			}
		};
	}
	//
	@Override
	protected DirectoryIOTask<T, C> getIOTask(final C item, final String nextNodeAddr) {
		return new BasicDirectoryIOTask<>(item, (FileIOConfig<T, C>) ioConfigCopy);
	}
	//
	@Override
	public int submitTasks(final List<? extends IOTask<C>> tasks, final int from, final int to)
	throws RemoteException, RejectedExecutionException {
		int n = 0;
		for(int i = from; i < to; i ++) {
			if(null != submitReq(tasks.get(i))) {
				n ++;
			} else {
				break;
			}
		}
		return n;
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected <A extends IOTask<C>> Future<A> submitTaskActually(final A ioTask)
	throws RejectedExecutionException {
		return (Future<A>) ioTaskExecutor
			.<DirectoryIOTask<T, C>>submit((DirectoryIOTask<T, C>) ioTask);
	}
	//
	@Override
	protected void shutdownActually() {
		if(!isCircular) {
			ioTaskExecutor.shutdown();
		}
		super.shutdownActually();
	}
	//
	@Override
	protected void interruptActually() {
		LOG.debug(Markers.MSG, "Dropped {} active I/O tasks", ioTaskExecutor.shutdownNow().size());
		super.interruptActually();
	}
}
