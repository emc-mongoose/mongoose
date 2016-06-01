package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.io.IoWorker;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
import com.emc.mongoose.core.api.io.task.DirectoryIoTask;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.load.executor.DirectoryLoadExecutor;
//
import com.emc.mongoose.core.impl.io.task.BasicDirectoryIoTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
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
extends LoadExecutorBase<C>
implements DirectoryLoadExecutor<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final ExecutorService ioTaskExecutor;
	//
	public BasicDirectoryLoadExecutor(
		final AppConfig appConfig, final FileIoConfig<T, C> ioConfig, final int threadCount,
		final Input<C> itemInput, final long countLimit, final long sizeLimit, final float rateLimit
	) throws ClassCastException {
		super(appConfig, ioConfig, null, threadCount, itemInput, countLimit, sizeLimit, rateLimit);
		ioTaskExecutor = new ThreadPoolExecutor(
			threadCount, threadCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(maxItemQueueSize), new IoWorker.Factory(getName())
		) {
			@Override @SuppressWarnings("unchecked")
			protected final <V> RunnableFuture<V> newTaskFor(final Runnable task, final V value) {
				return (RunnableFuture<V>) task;
			}
			//
			@Override @SuppressWarnings("unchecked")
			protected final void afterExecute(final Runnable task, final Throwable throwable) {
				final DirectoryIoTask<T, C> ioTask = (DirectoryIoTask<T, C>) task;
				if(throwable == null) {
					try {
						ioTaskCompleted(ioTask);
					} catch(final RemoteException ignored) {
					}
				} else {
					ioTaskFailed(1, throwable);
				}
			}
		};
	}
	//
	protected void logTrace(
		final String nodeAddr, final T item, final IoTask.Status status,
		final long reqTimeStart, final long countBytesDone, final int reqDuration,
		final int respLatency, final long respDataLatency
	) {
		if(LOG.isInfoEnabled(Markers.PERF_TRACE)) {
			StringBuilder strBuilder = PERF_TRACE_MSG_BUILDER.get();
			if(strBuilder == null) {
				strBuilder = new StringBuilder();
				PERF_TRACE_MSG_BUILDER.set(strBuilder);
			} else {
				strBuilder.setLength(0); // clear/reset
			}
			final String itemPath = item.getPath();
			LOG.info(
				Markers.PERF_TRACE,
				strBuilder
					//.append(loadType).append(',')
					.append(nodeAddr == null ? "" : nodeAddr).append(',')
					.append(
						itemPath == null ?
							item.getName() :
							itemPath.endsWith(File.separator) ?
								itemPath + item.getName() :
								itemPath + File.separator + item.getName()
					)
					.append(',')
					.append(countBytesDone).append(',')
					.append(status.code).append(',')
					.append(reqTimeStart).append(',')
					.append(respLatency > 0 ? respLatency : 0).append(',')
					.append(respDataLatency).append(',')
					.append(reqDuration)
					.toString()
			);
		}
	}
	//
	@Override
	protected DirectoryIoTask<T, C> getIoTask(final C item, final String nextNodeAddr) {
		return new BasicDirectoryIoTask<>(item, (FileIoConfig<T, C>) ioConfig);
	}
	//
	@Override
	public <A extends IoTask<C>> int submitTasks(final List<A> tasks, final int from, final int to)
	throws RemoteException, RejectedExecutionException {
		int n = 0;
		for(int i = from; i < to; i ++) {
			if(null != submitTask(tasks.get(i))) {
				n ++;
			} else {
				break;
			}
		}
		return n;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final <A extends IoTask<C>> Future<A> submitTask(final A ioTask)
	throws RejectedExecutionException {
		return (Future<A>) ioTaskExecutor
			.<DirectoryIoTask<T, C>>submit((DirectoryIoTask<T, C>) ioTask);
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
