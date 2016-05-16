package com.emc.mongoose.client.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.model.LoadState;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.executor.LoadExecutorBase;
import com.emc.mongoose.core.impl.load.tasks.AwaitLoadJobTask;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.impl.load.metrics.model.AggregatedRemoteIOStats;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 20.10.14.
 */
public abstract class LoadClientBase<T extends Item, W extends LoadSvc<T>>
extends LoadExecutorBase<T>
implements LoadClient<T, W> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final Map<String, W> remoteLoadMap;
	private final ThreadPoolExecutor remotePutExecutor;
	private final String loadSvcAddrs[];
	//
	protected volatile Output<T> consumer = null;
	//
	private final static int COUNT_LIMIT_RETRY = 100;
	//
	private final class LoadItemsBatchTask
	implements Runnable {
		//
		private final W loadSvc;
		//
		private LoadItemsBatchTask(final W loadSvc) {
			this.loadSvc = loadSvc;
		}
		//
		@Override
		public final void run() {
			//
			final Thread currThread = Thread.currentThread();
			currThread.setName("itemsBatchLoader<" + getName() + ">");
			//
			int retryCount = 0;
			try {
				while(!currThread.isInterrupted()) {
					try {
						List<T> frame = loadSvc.getProcessedItems();
						retryCount = 0; // reset
						if(frame == null) {
							LOG.debug(
								Markers.ERR, "No data items frame from the load server @ {}",
								loadSvc
							);
						} else {
							final int n = frame.size();
							if(n > 0) {
								if(LOG.isTraceEnabled(Markers.MSG)) {
									LOG.trace(
										Markers.MSG,
										"Got the next {} items from the load server @ {}",
										n, loadSvc
									);
								}
								counterResults.addAndGet(n);
								// CIRCULARITY FEATURE
								if(isCircular) {
									for(final T item : frame) {
										uniqueItems.put(item.getName(), item);
									}
								}
								for(int m = 0; m < n && !currThread.isInterrupted();) {
									m += itemOutBuff.put(frame, m, n);
									LockSupport.parkNanos(1);
									Thread.yield();
								}
								if(LOG.isTraceEnabled(Markers.MSG)) {
									LOG.trace(
										Markers.MSG, "Put the next {} items to the output buffer",
										n, loadSvc
									);
								}
							} else {
								if(LOG.isTraceEnabled(Markers.MSG)) {
									LOG.trace(
										Markers.MSG,
										"No data items in the frame from the load server @ {}",
										loadSvc
									);
								}
							}
						}
						LockSupport.parkNanos(1);
						Thread.yield();
					} catch(final IOException e) {
						if(retryCount < COUNT_LIMIT_RETRY) {
							retryCount ++;
							TimeUnit.MILLISECONDS.sleep(retryCount);
						} else {
							LogUtil.exception(
								LOG, Level.ERROR, e,
								"Failed to load the processed items from the load server @ {}",
								loadSvc
							);
							break;
						}
					}
				}
			} catch(final InterruptedException ignored) {
			}
		}
	}
	//
	private final class InterruptOnCountLimitReachedTask
	implements Runnable {
		@Override
		public final void run() {
			final Thread currThread = Thread.currentThread();
			currThread.setName("interruptOnCountLimitReached<" + getName() + ">");
			if(countLimit > 0) {
				try {
					while(!currThread.isInterrupted()) {
						if(countLimit <= lastStats.getSuccCount() + lastStats.getFailCount()) {
							LOG.debug(
								Markers.MSG, "Interrupting due to count limit ({}) is reached",
								countLimit
							);
							break;
						} else if(currThread.isInterrupted()) {
							LOG.debug(Markers.MSG, "Interrupting due to external interruption");
							break;
						} else {
							LockSupport.parkNanos(1);
							Thread.yield();
						}
					}
				} finally {
					isLimitReached = true;
					remotePutExecutor.shutdownNow();
					interruptLoadSvcs();
				}
			}
		}
	}
	//
	private final class InterruptOnSizeLimitReachedTask
	implements Runnable {
		@Override
		public final void run() {
			final Thread currThread = Thread.currentThread();
			currThread.setName("interruptOnCountLimitReached<" + getName() + ">");
			if(sizeLimit > 0) {
				try {
					while(!currThread.isInterrupted()) {
						if(sizeLimit <= lastStats.getByteCount()) {
							LOG.debug(
								Markers.MSG, "Interrupting due to size limit ({}) is reached",
								SizeInBytes.formatFixedSize(sizeLimit)
							);
							break;
						} else if(currThread.isInterrupted()) {
							LOG.debug(Markers.MSG, "Interrupting due to external interruption");
							break;
						} else {
							LockSupport.parkNanos(1);
							Thread.yield();
						}
					}
				} finally {
					isLimitReached = true;
					remotePutExecutor.shutdownNow();
					interruptLoadSvcs();
				}
			}
		}
	}
	//
	public LoadClientBase(
		final AppConfig appConfig, final IoConfig<?, ?> ioConfig, final String addrs[],
		final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		this(
			appConfig, ioConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit,
			// get any load server last job number
			remoteLoadMap, remoteLoadMap.values().iterator().next().getInstanceNum()
		);
	}
	//
	protected LoadClientBase(
		final AppConfig appConfig, final IoConfig<?, ?> ioConfig, final String addrs[],
		final int threadCount, final Input<T> itemInput,
		final long countLimit, final long sizeLimit, final float rateLimit,
		final Map<String, W> remoteLoadMap, final int instanceNum
	) {
		super(
			appConfig, ioConfig, addrs, threadCount, itemInput, countLimit, sizeLimit, rateLimit,
			instanceNum,
			instanceNum + "-" + ioConfig.toString() +
				(countLimit > 0 ? Long.toString(countLimit) : "") + '-' + threadCount +
				(addrs == null ? "" : 'x' + Integer.toString(addrs.length))
				+ 'x' + remoteLoadMap.size()
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		this.remoteLoadMap = remoteLoadMap;
		this.loadSvcAddrs = new String[remoteLoadMap.size()];
		remoteLoadMap.keySet().toArray(this.loadSvcAddrs);
		////////////////////////////////////////////////////////////////////////////////////////////
		for(final W nextLoadSvc : remoteLoadMap.values()) {
			mgmtTasks.add(new LoadItemsBatchTask(nextLoadSvc));
		}
		if(this.countLimit > 0 && this.countLimit < Long.MAX_VALUE) {
			mgmtTasks.add(new InterruptOnCountLimitReachedTask());
		}
		if(sizeLimit > 0 && sizeLimit < Long.MAX_VALUE) {
			mgmtTasks.add(new InterruptOnSizeLimitReachedTask());
		}
		//
		final int remotePutThreadCount = Math.max(loadSvcAddrs.length, ThreadUtil.getWorkerCount());
		remotePutExecutor = new ThreadPoolExecutor(
			remotePutThreadCount, remotePutThreadCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(DEFAULT_SUBM_TASKS_QUEUE_SIZE),
			new NamingThreadFactory(getName() + "-put-remote", true)
		);
	}
	//
	@Override
	protected final void initStats(final boolean flagServeJMX) {
		ioStats = new AggregatedRemoteIOStats<>(getName(), flagServeJMX, remoteLoadMap);
		lastStats = ioStats.getSnapshot();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Producer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected void startActually() {
		//
		W nextLoadSvc;
		for(final String addr : loadSvcAddrs) {
			nextLoadSvc = remoteLoadMap.get(addr);
			try {
				nextLoadSvc.start();
				LOG.debug(
					Markers.MSG, "{} started bound to remote service @{}",
					nextLoadSvc.getName(), addr
				);
			} catch(final IOException | IllegalStateException e) {
				try {
					LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to start remote load \"{}\" @{}",
						nextLoadSvc.getName(), addr
					);
				} catch(final RemoteException ee) {
					LogUtil.exception(LOG, Level.FATAL, e, "Network connectivity failure");
				}
			}
		}
		//
		super.startActually();
	}
	//
	private final static class InterruptSvcTask
	implements Runnable {
		//
		private final LoadSvc loadSvc;
		private final String addr;
		//
		private InterruptSvcTask(final LoadSvc loadSvc, final String addr) {
			this.loadSvc = loadSvc;
			this.addr = addr;
		}
		//
		@Override
		public final void run() {
			try {
				loadSvc.shutdown();
				// wait until all processed items are received from the load server
				int n;
				do {
					n = loadSvc.getProcessedItemsCount();
					if(n > 0) {
						if(LOG.isTraceEnabled(Markers.MSG)) {
							LOG.trace(Markers.MSG, "{}: {} processed items remaining", addr, n);
						}
						TimeUnit.MILLISECONDS.sleep(10);
					} else {
						break;
					}
				} while(true);
				LOG.debug(
					Markers.MSG, "All processed items have been received from load service @ {}",
					addr
				);
			} catch(final InterruptedException | RemoteException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e, "Waiting for remote processed items @ {} was interrupted",
					addr
				);
			} finally { // ok, continue
				try {
					loadSvc.interrupt();
					LOG.debug(Markers.MSG, "Interrupted remote service @ {}", addr);
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.DEBUG, e, "Failed to interrupt remote load service @ {}", addr
					);
				}
			}
		}
	}
	//
	private void interruptLoadSvcs() {
		//
		final int loadSvcCount = remoteLoadMap.size();
		if(loadSvcCount > 0) {
			final ExecutorService interruptExecutor = Executors.newFixedThreadPool(
				remoteLoadMap.size(),
				new NamingThreadFactory(String.format("interrupt<%s>", getName()), true)
			);
			for(final String addr : loadSvcAddrs) {
				interruptExecutor.submit(new InterruptSvcTask(remoteLoadMap.get(addr), addr));
			}
			interruptExecutor.shutdown();
			try {
				if(!interruptExecutor.awaitTermination(REMOTE_TASK_TIMEOUT_SEC, TimeUnit.SECONDS)) {
					LOG.warn(Markers.ERR, "{}: remote interrupt tasks timeout", getName());
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Interrupting interrupted %<");
			} finally {
				final List<Runnable> tasksLeft = interruptExecutor.shutdownNow();
				for(final Runnable task : tasksLeft) {
					LOG.debug(Markers.ERR, "The interrupt task is not finished in time: {}", task);
				}
			}
		}
	}
	//
	@Override
	protected void interruptActually() {
		try {
			remotePutExecutor.shutdownNow();
			interruptLoadSvcs();
		} finally {
			super.interruptActually();
		}
	}
	//
	@Override
	public final void setOutput(final Output<T> itemOutput)
	throws RemoteException {
		if(itemOutput instanceof LoadClient) {
			LOG.debug(Markers.MSG, "Consumer is a LoadClient instance");
			// consumer is client which has the map of consumers
			// this is necessary for the distributed chain/rampup scenarios
			this.consumer = itemOutput;
			final Map<String, W> consumeMap = ((LoadClient<T, W>)itemOutput)
				.getRemoteLoadMap();
			for(final String addr : consumeMap.keySet()) {
				remoteLoadMap.get(addr).setOutput(consumeMap.get(addr));
			}
		} else if(itemOutput instanceof LoadSvc) {
			// single consumer for all these producers
			final W loadSvc = (W)itemOutput;
			LOG.debug(Markers.MSG, "Consumer is a load service");
			for(final String addr : loadSvcAddrs) {
				remoteLoadMap.get(addr).setOutput(loadSvc);
			}
		} else {
			super.setOutput(itemOutput);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final AtomicLong rrc = new AtomicLong(0);
	//
	@Deprecated
	private final class RemotePutTask
	implements Runnable {
		//
		private final T dataItem;
		//
		private RemotePutTask(final T dataItem) {
			this.dataItem = dataItem;
		}
		//
		@Override
		public final void run() {
			String loadSvcAddr;
			for(
				int tryCount = 0;
				tryCount < Short.MAX_VALUE;
				tryCount ++
			) {
				try {
					loadSvcAddr = loadSvcAddrs[
						(int) (rrc.incrementAndGet() % loadSvcAddrs.length)
					];
					remoteLoadMap.get(loadSvcAddr).put(dataItem);
					counterSubm.incrementAndGet();
					break;
				} catch(final RejectedExecutionException | IOException e) {
					if(remotePutExecutor.isTerminated()) {
						break;
					} else {
						try {
							Thread.sleep(tryCount);
						} catch(final InterruptedException ee) {
							break;
						}
					}
				}
			}
		}
	}
	//
	@Override
	public void put(final T item)
	throws IOException {
		if(counterSubm.get() + countRej.get() >= countLimit) {
			LOG.debug(
				Markers.MSG, "{}: all tasks has been submitted ({}) or rejected ({})", getName(),
				counterSubm.get(), countRej.get()
			);
			if(isShutdown.compareAndSet(false, true)) {
				shutdownActually();
			}
			return;
		}
		if(remotePutExecutor.isShutdown()) {
			return;
		}
		while(true) {
			try {
				remotePutExecutor.submit(new RemotePutTask(item));
				break;
			} catch(final RejectedExecutionException e) {
				if(LOG.isTraceEnabled(Markers.ERR)) {
					LogUtil.exception(LOG, Level.TRACE, e, "Failed to submit remote put task");
				}
				LockSupport.parkNanos(1);
			}
		}
	}
	//
	private final class RemoteBatchPutTask
	implements Runnable {
		//
		private final List<T> items;
		private final int from, to, n;
		//
		private RemoteBatchPutTask(final List<T> items, final int from, final int to) {
			this.items = items;
			this.from = from;
			this.to = to;
			this.n = to - from;
		}
		//
		@Override
		public final void run() {
			String loadSvcAddr;
			W loadSvc;
			int m = 0;
			for(
				int tryCount = 0;
				tryCount < Short.MAX_VALUE;
				tryCount ++
			) {
				try {
					loadSvcAddr = loadSvcAddrs[
						(int) (rrc.incrementAndGet() % loadSvcAddrs.length)
					];
					loadSvc = remoteLoadMap.get(loadSvcAddr);
					do {
						m += loadSvc.put(items, from + m, to);
						if(m < n) {
							LockSupport.parkNanos(1);
						} else {
							break;
						}
					} while(!remotePutExecutor.isShutdown());
					counterSubm.addAndGet(n);
					break;
				} catch(final IOException e) {
					if(remotePutExecutor.isTerminated()) {
						break;
					} else {
						try {
							Thread.sleep(tryCount);
						} catch(final InterruptedException ee) {
							break;
						}
					}
				}
			}
		}
	}
	//
	@Override
	public int put(final List<T> dataItems, final int from, final int to)
	throws IOException {
		final long dstLimit = countLimit - counterSubm.get() - countRej.get();
		final int srcLimit = to - from;
		if(dstLimit > 0) {
			if(dstLimit < srcLimit) {
				return put(dataItems, from, from + (int) dstLimit);
			} else {
				if(remotePutExecutor.isShutdown()) {
					return 0;
				}
				while(true) {
					try {
						remotePutExecutor.submit(new RemoteBatchPutTask(dataItems, from, to));
						break;
					} catch(final RejectedExecutionException e) {
						if(remotePutExecutor.isShutdown()) {
							return 0;
						}
						if(LOG.isTraceEnabled(Markers.ERR)) {
							LogUtil.exception(
								LOG, Level.TRACE, e, "Failed to submit remote batch put task"
							);
						}
						LockSupport.parkNanos(1_000);
					}
				}
			}
		} else {
			if(isShutdown.compareAndSet(false, true)) {
				shutdownActually();
			}
			if(srcLimit > 0) {
				countRej.addAndGet(srcLimit);
				LOG.debug(Markers.MSG, "Rejected {} I/O tasks", srcLimit);
			}
		}
		return to - from;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void setLoadState(final LoadState<T> state) {
		if(state != null) {
			LOG.warn(Markers.MSG, "Failed to resume run in distributed mode. See jira ticket #411");
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected void closeActually()
	throws IOException {
		try {
			if(isInterrupted.compareAndSet(false, true)) {
				synchronized(state) {
					state.notifyAll();
				}
				interruptActually();
			}
		} finally {
			if(!remoteLoadMap.isEmpty()) {
				LOG.debug(Markers.MSG, "{}: closing the remote services...", getName());
				W nextLoadSvc;
				for(final String addr : remoteLoadMap.keySet()) {
					//
					try {
						nextLoadSvc = remoteLoadMap.get(addr);
						nextLoadSvc.close();
						LOG.debug(Markers.MSG, "Server instance @ {} has been closed", addr);
					} catch(final NoSuchElementException e) {
						if(!remotePutExecutor.isTerminated()) {
							LOG.debug(
								Markers.ERR, "Looks like the remote load service is already closed"
							);
						}
					} catch(final NoSuchObjectException e) {
						LogUtil.exception(
							LOG, Level.DEBUG, e, "No remote service found for closing"
						);
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to close remote load executor service"
						);
					}
				}
				//
				super.closeActually(); // requires the remoteLoadMap to be not empty yet
				LOG.debug(Markers.MSG, "Clear the servers map");
				remoteLoadMap.clear();
			} else {
				LOG.debug(Markers.MSG, "{}: closed already", getName());
			}
		}
	}
	//
	@Override
	public final Map<String, W> getRemoteLoadMap() {
		return remoteLoadMap;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected void shutdownActually() {
		try {
			super.shutdownActually();
		} finally {
			LOG.debug(Markers.MSG, "{}: shutdown invoked", getName());
			// CIRCULARITY: why shutdown is disabled?
			if(!isCircular) {
				final long timeOutSec = appConfig.getLoadLimitTime();
				remotePutExecutor.shutdown();
				try {
					if(
						!remotePutExecutor.awaitTermination(
							timeOutSec > 0 ? timeOutSec : Long.MAX_VALUE, TimeUnit.SECONDS
						)
					) {
						LOG.debug(
							Markers.ERR,
							"Timeout while submitting all the remaining data items to the load servers"
						);
					}
				} catch(final InterruptedException e) {
					LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted");
				} finally {
					LOG.debug(
						Markers.MSG, "Submitted {} items to the load servers",
						remotePutExecutor.getCompletedTaskCount()
					);
					for(final String addr : remoteLoadMap.keySet()) {
						try {
							remoteLoadMap.get(addr).shutdown();
						} catch(final NoSuchObjectException ignored) {
						} catch(final RemoteException e) {
							LogUtil.exception(
								LOG, Level.WARN, e, "Failed to shut down remote load service"
							);
						}
					}
				}
			}
		}
	}
	//
	@Override
	public <A extends IOTask<T>> Future<A> submitTask(final A request)
	throws RemoteException {
		return remoteLoadMap
			.get(loadSvcAddrs[(int) (remotePutExecutor.getTaskCount() % loadSvcAddrs.length)])
			.submitTask(request);
	}
	//
	@Override
	public <A extends IOTask<T>> int submitTasks(
		final List<A> requests, final int from, final int to
	) throws RemoteException, RejectedExecutionException {
		return remoteLoadMap
			.get(loadSvcAddrs[(int) (remotePutExecutor.getTaskCount() % loadSvcAddrs.length)])
			.submitTasks(requests, from, to);
	}
	//
	@Override
	public void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {
		//
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
			remoteLoadMap.size() + 1,
			new NamingThreadFactory(String.format("awaitWorker<%s>", getName()), true)
		);
		awaitExecutor.submit(
			new Runnable() {
				@Override
				public void run() {
					// wait the remaining tasks to be transmitted to load servers
					LOG.debug(
						Markers.MSG, "{}: waiting remaining {} tasks to complete", LoadClientBase.this.getName(),
						remotePutExecutor.getQueue().size() + remotePutExecutor.getActiveCount()
					);
					try {
						if(!remotePutExecutor.awaitTermination(timeOut, timeUnit)) {
							remotePutExecutor.shutdownNow();
						}
					} catch(final InterruptedException e) {
						LOG.debug(Markers.MSG, "Interrupted");
					}
				}
			}
		);
		for(final String addr : remoteLoadMap.keySet()) {
			awaitExecutor.submit(new AwaitLoadJobTask(remoteLoadMap.get(addr), timeOut, timeUnit));
		}
		awaitExecutor.shutdown();
		try {
			LOG.debug(Markers.MSG, "Wait remote await tasks for finish {}[{}]", timeOut, timeUnit);
			if(awaitExecutor.awaitTermination(timeOut, timeUnit)) {
				LOG.debug(Markers.MSG, "All await tasks finished");
			} else {
				LOG.debug(Markers.MSG, "Await tasks execution timeout");
			}
		} finally {
			LOG.debug(
				Markers.MSG, "Interrupted await tasks: {}",
				Arrays.toString(awaitExecutor.shutdownNow().toArray())
			);
		}
	}
}
