package com.emc.mongoose.client.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemDst;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.model.LoadState;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.item.data.NewDataItemSrc;
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
	protected volatile ItemDst<T> consumer = null;
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
			currThread.setName("dataItemsBatchLoader<" + getName() + ">");
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
								if(isCircular) {
									for(final T item : frame) {
										uniqueItems.put(item.getName(), item);
									}
								}
								for(int m = 0; m < n && !currThread.isInterrupted();) {
									m += itemOutBuff.put(frame, m, n);
									LockSupport.parkNanos(1);
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
						Thread.yield(); LockSupport.parkNanos(1);
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
			if(maxCount > 0) {
				try {
					while(!currThread.isInterrupted()) {
						if(maxCount <= lastStats.getSuccCount() + lastStats.getFailCount()) {
							LOG.debug(
								Markers.MSG, "Interrupting due to count limit ({}) is reached",
								maxCount
							);
							break;
						} else if(currThread.isInterrupted()) {
							LOG.debug(Markers.MSG, "Interrupting due to external interruption");
							break;
						} else {
							LockSupport.parkNanos(1); Thread.yield();
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
	@SuppressWarnings("unchecked")
	public LoadClientBase(
		final RunTimeConfig rtConfig, final IOConfig<?, ?> ioConfig, final String addrs[],
		final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount,
		final Map<String, W> remoteLoadMap
	) throws RemoteException {
		super(
			rtConfig, ioConfig, addrs, connCountPerNode, threadCount,
			// suppress new data items generation on the client side
			itemSrc instanceof NewDataItemSrc ? null : itemSrc, maxCount,
			// get any load server last job number
			remoteLoadMap.values().iterator().next().getInstanceNum(),
			remoteLoadMap.values().iterator().next().getName() + 'x' + remoteLoadMap.size()
		);
		////////////////////////////////////////////////////////////////////////////////////////////
		this.remoteLoadMap = remoteLoadMap;
		this.loadSvcAddrs = new String[remoteLoadMap.size()];
		remoteLoadMap.keySet().toArray(this.loadSvcAddrs);
		////////////////////////////////////////////////////////////////////////////////////////////
		for(final W nextLoadSvc : remoteLoadMap.values()) {
			mgmtTasks.add(new LoadItemsBatchTask(nextLoadSvc));
		}
		mgmtTasks.add(new InterruptOnCountLimitReachedTask());
		//
		final int remotePutThreadCount = Math.max(loadSvcAddrs.length, ThreadUtil.getWorkerCount());
		remotePutExecutor = new ThreadPoolExecutor(
			remotePutThreadCount, remotePutThreadCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(DEFAULT_SUBM_TASKS_QUEUE_SIZE),
			new GroupThreadFactory(getName() + "-put-remote", true)
		);
	}
	//
	@Override
	protected final void initStats(final boolean flagServeJMX) {
		if(flagServeJMX) {
			ioStats = new AggregatedRemoteIOStats<>(
				getName(), rtConfig.getRemotePortMonitor(), remoteLoadMap
			);
		} else {
			ioStats = new AggregatedRemoteIOStats<>(getName(), 0, remoteLoadMap);
		}
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
			} catch(final IOException e) {
				LOG.error(Markers.ERR, "Failed to start remote load @" + addr, e);
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
		final ExecutorService interruptExecutor = Executors.newFixedThreadPool(
			remoteLoadMap.size(),
			new GroupThreadFactory(String.format("interrupt<%s>", getName()))
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
			interruptExecutor.shutdownNow();
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
	public final void setItemDst(final ItemDst<T> itemDst)
	throws RemoteException {
		if(itemDst instanceof LoadClient) {
			LOG.debug(Markers.MSG, "Consumer is a LoadClient instance");
			// consumer is client which has the map of consumers
			// this is necessary for the distributed chain/rampup scenarios
			this.consumer = itemDst;
			final Map<String, W> consumeMap = ((LoadClient<T, W>) itemDst)
				.getRemoteLoadMap();
			for(final String addr : consumeMap.keySet()) {
				remoteLoadMap.get(addr).setItemDst(consumeMap.get(addr));
			}
		} else if(itemDst instanceof LoadSvc) {
			// single consumer for all these producers
			final W loadSvc = (W) itemDst;
			LOG.debug(Markers.MSG, "Consumer is a load service");
			for(final String addr : loadSvcAddrs) {
				remoteLoadMap.get(addr).setItemDst(loadSvc);
			}
		} else {
			super.setItemDst(itemDst);
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
	public final void put(final T dataItem)
	throws IOException {
		if(remotePutExecutor.isShutdown()) {
			return;
		}
		while(true) {
			try {
				remotePutExecutor.submit(new RemotePutTask(dataItem));
				break;
			} catch(final RejectedExecutionException e) {
				if(LOG.isTraceEnabled(Markers.ERR)) {
					LogUtil.exception(LOG, Level.TRACE, e, "Failed to submit remote put task");
				}
				Thread.yield();
			}
		}
	}
	//
	private final class RemoteBatchPutTask
	implements Runnable {
		//
		private final List<T> dataItems;
		private final int from, to, n;
		//
		private RemoteBatchPutTask(final List<T> dataItems, final int from, final int to) {
			this.dataItems = dataItems;
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
						m += loadSvc.put(dataItems, from + m, to);
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
	public final int put(final List<T> dataItems, final int from, final int to)
	throws IOException {
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
					LogUtil.exception(LOG, Level.TRACE, e, "Failed to submit remote batch put task");
				}
				Thread.yield();
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
	public final void closeActually()
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
			//
			if(!isCircular) {
				final long timeOut = rtConfig.getLoadLimitTimeValue();
				final TimeUnit timeUnit = rtConfig.getLoadLimitTimeUnit();
				remotePutExecutor.shutdown();
				try {
					if(
						!remotePutExecutor.awaitTermination(
							timeOut > 0 ? timeOut : Long.MAX_VALUE,
							timeUnit == null ? TimeUnit.DAYS : timeUnit
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
	public final <A extends IOTask<T>> Future<A> submitReq(final A request)
	throws RemoteException {
		return remoteLoadMap
			.get(loadSvcAddrs[(int) (remotePutExecutor.getTaskCount() % loadSvcAddrs.length)])
			.submitReq(request);
	}
	//
	@Override
	public final int submitTasks(
		final List<? extends IOTask<T>> requests, final int from, final int to
	) throws RemoteException, RejectedExecutionException {
		return remoteLoadMap
			.get(loadSvcAddrs[(int) (remotePutExecutor.getTaskCount() % loadSvcAddrs.length)])
			.submitTasks(requests, from, to);
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {
		//
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(
			remoteLoadMap.size() + 1,
			new GroupThreadFactory(String.format("awaitWorker<%s>", getName()))
		);
		awaitExecutor.submit(
			new Runnable() {
				@Override
				public final void run() {
					// wait the remaining tasks to be transmitted to load servers
					LOG.debug(
						Markers.MSG, "{}: waiting remaining {} tasks to complete", getName(),
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
