package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.IOConfig;
import com.emc.mongoose.core.api.io.task.DirectoryIOTask;
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.core.impl.io.task.BasicDirectoryIOTask;
//
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 23.11.15.
 */
public class BasicDirectoryLoadExecutor<T extends FileItem, C extends Directory<T>>
extends LimitedRateLoadExecutorBase<C> {
	//
	private final ExecutorService ioTaskExecutor;
	//
	public BasicDirectoryLoadExecutor(
		final RunTimeConfig rtConfig,
		final IOConfig<? extends FileItem, ? extends Directory<? extends FileItem>> ioConfig,
		final String[] addrs, final int connCountPerNode, final int threadCount,
		final ItemSrc<C> itemSrc, final long maxCount,
		final int manualTaskSleepMicroSecs, final float rateLimit
	) throws ClassCastException {
		super(
			rtConfig, ioConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			manualTaskSleepMicroSecs, rateLimit
		);
		ioTaskExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(maxItemQueueSize)
		);
	}
	//
	@Override
	protected DirectoryIOTask<T, C> getIOTask(final C item, final String nextNodeAddr) {
		return new BasicDirectoryIOTask<>(item, (IOConfig<T, C>) ioConfigCopy);
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
	@Override
	protected <A extends IOTask<C>> Future<A> submitTaskActually(final A ioTask)
	throws RejectedExecutionException {
		return (Future<A>) ioTaskExecutor
			.<DirectoryIOTask<T, C>>submit((DirectoryIOTask<T, C>) ioTask);
	}
}
