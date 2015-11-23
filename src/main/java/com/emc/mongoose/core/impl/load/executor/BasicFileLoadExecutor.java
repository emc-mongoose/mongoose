package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.IOConfig;
import com.emc.mongoose.core.api.io.task.FileIOTask;
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.core.impl.io.task.BasicFileIOTask;
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
public class BasicFileLoadExecutor<T extends FileItem>
extends MutableDataLoadExecutorBase<T> {
	//
	private final ExecutorService ioTaskExecutor;
	//
	public BasicFileLoadExecutor(
		final RunTimeConfig rtConfig,
		final IOConfig<? extends FileItem, ? extends Directory<? extends FileItem>> ioConfig,
		final String[] addrs, final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias,
		final int manualTaskSleepMicroSecs, final float rateLimit, final int countUpdPerReq
	) throws ClassCastException {
		super(
			rtConfig, ioConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount,
			sizeMin, sizeMax, sizeBias, manualTaskSleepMicroSecs, rateLimit, countUpdPerReq
		);
		ioTaskExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(maxItemQueueSize)
		);
	}
	//
	@Override
	protected FileIOTask<T> getIOTask(final T item, final String nextNodeAddr) {
		return new BasicFileIOTask<>(item, (IOConfig<T, Directory<T>>) ioConfigCopy);
	}
	//
	@Override
	public int submitTasks(final List<? extends IOTask<T>> tasks, final int from, final int to)
	throws RemoteException, RejectedExecutionException {
		ioTaskExecutor.invokeAny(tasks)
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
	protected Future<? extends FileIOTask<T>> submitTaskActually(final IOTask<T> ioTask)
	throws RejectedExecutionException {
		return null;
	}
}
