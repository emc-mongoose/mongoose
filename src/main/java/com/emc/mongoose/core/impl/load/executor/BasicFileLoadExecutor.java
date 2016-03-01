package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.io.IOWorker;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.io.task.FileIOTask;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
//
import com.emc.mongoose.core.impl.io.task.BasicFileIOTask;
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
public class BasicFileLoadExecutor<T extends FileItem>
extends MutableDataLoadExecutorBase<T>
implements FileLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final ExecutorService ioTaskExecutor;
	//
	public BasicFileLoadExecutor(
		final AppConfig appConfig, final FileIOConfig<T, ? extends Directory<T>> ioConfig,
		final String[] addrs, final int threadCount, final ItemSrc<T> itemSrc, final long maxCount,
		final float rateLimit, final DataRangesConfig rangesConfig
	) throws ClassCastException {
		super(appConfig, ioConfig, addrs, threadCount, itemSrc, maxCount, rateLimit, rangesConfig);
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
				if(throwable == null) {
					ioTaskCompleted((FileIOTask<T>) task);
				} else {
					ioTaskFailed(1, throwable);
				}
			}
		};
	}
	//
	@Override
	protected FileIOTask<T> getIOTask(final T item, final String nextNodeAddr) {
		return new BasicFileIOTask<>(item, (FileIOConfig<T, Directory<T>>) ioConfigCopy);
	}
	//
	@Override
	public int submitTasks(final List<? extends IOTask<T>> tasks, final int from, final int to)
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
	@Override
	protected <A extends IOTask<T>> Future<A> submitTaskActually(final A ioTask)
	throws RejectedExecutionException {
		return (Future<A>) ioTaskExecutor.<FileIOTask<T>>submit((FileIOTask<T>) ioTask);
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
