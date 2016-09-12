package com.emc.mongoose.monitor;

import com.emc.mongoose.common.concurrent.InterruptibleDaemonBase;
import com.emc.mongoose.model.api.item.ItemBuffer;
import com.emc.mongoose.model.impl.item.LimitedQueueItemBuffer;
import com.emc.mongoose.model.impl.metrics.BasicIoStats;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import static com.emc.mongoose.ui.config.Config.LoadConfig.MetricsConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.api.load.Generator;
import com.emc.mongoose.model.api.load.Monitor;
import com.emc.mongoose.model.api.metrics.IoStats;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.emc.mongoose.model.api.item.Item.SLASH;

/**
 Created by kurila on 12.07.16.
 */
public class BasicMonitor<I extends Item, O extends IoTask<I>>
extends InterruptibleDaemonBase
implements Monitor<I, O> {

	private final static Logger LOG = LogManager.getLogger();
	private final String name;
	private final List<Generator<I, O>> generators;
	private final ConcurrentMap<String, Driver<I, O>> drivers = new ConcurrentHashMap<>();
	private final MetricsConfig metricsConfig;
	private final long countLimit;
	private final long sizeLimit;
	private final boolean isCircular;
	private final int batchSize = 0x1000;
	private final ItemBuffer<I> itemOutBuff;
	private final IoStats ioStats, medIoStats;
	private volatile IoStats.Snapshot lastStats = null;
	private final Thread worker;
	
	public BasicMonitor(
		final String name, final List<Generator<I, O>> generators, final LoadConfig loadConfig
	) {
		this.name = name;
		this.generators = generators;
		for(final Generator<I, O> generator : generators) {
			generator.registerMonitor(this);
		}
		this.metricsConfig = loadConfig.getMetricsConfig();
		final int metricsPeriosSec = (int) metricsConfig.getPeriod();
		this.ioStats = new BasicIoStats(name, metricsPeriosSec);
		this.medIoStats = new BasicIoStats(name, metricsPeriosSec);
		final LimitConfig limitConfig = loadConfig.getLimitConfig();
		if(limitConfig.getCount() > 0) {
			countLimit = limitConfig.getCount();
		} else {
			countLimit = Long.MAX_VALUE;
		}
		if(limitConfig.getSize().get() > 0) {
			sizeLimit = limitConfig.getSize().get();
		} else {
			sizeLimit = Long.MAX_VALUE;
		}
		this.worker = new Thread(new ServiceTask(), name);
		this.worker.setDaemon(true);
		this.isCircular = loadConfig.getCircular();
		this.itemOutBuff = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<>(loadConfig.getQueueConfig().getSize())
		);
	}
	
	private final class ServiceTask
	implements Runnable {

		private long prevNanoTimeStamp;

		public ServiceTask() {
			this.prevNanoTimeStamp = -1;
		}

		@Override
		public final void run() {
			
			long nextNanoTimeStamp;
			
			boolean allGeneratorsDoneFlag;
			
			while(!isInterrupted()) {
				nextNanoTimeStamp = System.nanoTime();
				
				// refresh the stats
				lastStats = ioStats.getSnapshot();
				
				// output the current measurements every 10 sec
				if(nextNanoTimeStamp - prevNanoTimeStamp > 1e10) {
					LOG.info(Markers.PERF_AVG, lastStats.toString());
				}
				
				//
				allGeneratorsDoneFlag = true;
				for(final Generator<I, O> nextGenerator : generators) {
					if(!nextGenerator.isInterrupted()) {
						allGeneratorsDoneFlag = false;
						break;
					}
				}
				if(!isCircular || allGeneratorsDoneFlag) {
					postProcessItems();
				}
				
				prevNanoTimeStamp = nextNanoTimeStamp;
			}
		}
	}
	
	protected void postProcessItems() {
		try {
			final List<I> items = new ArrayList<>(batchSize);
			final int n = itemOutBuff.get(items, batchSize);
			if(n > 0) {
				if(isCircular) {
					int m = 0, k;
					while(m < n) {
						k = put(items, m, n);
						if(k > 0) {
							m += k;
						} else {
							break;
						}
						LockSupport.parkNanos(1_000);
					}
				} else {
					postProcessUniqueItemsFinally(items);
				}
			} else if(isDone()) {
				isPostProcessDone = true;
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", consumer);
		} catch(final RejectedExecutionException e) {
			if(LOG.isTraceEnabled(Markers.ERR)) {
				LogUtil.exception(LOG, Level.TRACE, e, "\"{}\" rejected the items", consumer);
			}
		}
	}
	//
	protected void postProcessUniqueItemsFinally(final List<I> items) {
		// is this an end of consumer-producer chain?
		if(consumer == null) {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "{}: going to dump out {} items", getName(), items.size());
			}
			if(LOG.isInfoEnabled(Markers.ITEM_LIST)) {
				try {
					for(final Item item : items) {
						LOG.info(Markers.ITEM_LIST, item.toString());
					}
				} catch(final Throwable e) {
					LogUtil.exception(
						LOG, Level.ERROR, e, "{}: failed to dump out {} items",
						getName(), items.size()
					);
				}
			}
		} else { // put to the consumer
			int n = items.size();
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Going to put {} items to the consumer {}",
					n, consumer
				);
			}
			try {
				for(int m = 0; m < n; m += consumer.put(items, m, n)) {
					LockSupport.parkNanos(1);
				}
				items.clear();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", consumer
				);
			}
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG,
					"{} items were passed to the consumer {} successfully",
					n, consumer
				);
			}
		}
	}
	
	protected final boolean isDoneCountLimit() {
		return countLimit > 0 && (
			counterResults.get() >= countLimit ||
				lastStats.getSuccCount() + lastStats.getFailCount() >= countLimit
		);
	}
	//
	protected final boolean isDoneSizeLimit() {
		return sizeLimit > 0 && lastStats.getByteCount() >= sizeLimit;
	}
	//
	protected boolean isDoneAllSubm() {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "{}: shut down flag: {}, results: {}, submitted: {}",
				getName(), isShutdown.get(), counterResults.get(), counterSubm.get()
			);
		}
		return isShutdown.get() && counterResults.get() >= counterSubm.get();
	}
	//
	private boolean isDone() {
		if(isDoneAllSubm()) {
			if(!isCircular) {
				LOG.debug(
					Markers.MSG, "{}: done due to \"done all submitted\" state",
					getName()
				);
				return true;
			}
		}
		if(isDoneCountLimit()) {
			LOG.debug(Markers.MSG, "{}: done due to max count done state", getName());
			return true;
		}
		if(isDoneSizeLimit()) {
			LOG.debug(Markers.MSG, "{}: done due to max size done state", getName());
			return true;
		}
		if(isLimitReached) {
			LOG.debug(Markers.MSG, "{}: await exit due to limits reached state", getName());
			return true;
		}
		return false;
	}
	
	
	@Override
	public void ioTaskCompleted(final O ioTask) {
		if(isInterrupted()) {
			return;
		}
		final I item = ioTask.getItem();
		final IoTask.Status status = ioTask.getStatus();
		final String nodeAddr = ioTask.getNodeAddr();
		final int reqDuration = ioTask.getDuration();
		final int respLatency = ioTask.getLatency();
		final int respDataLatency;
		final long countBytesDone;
		if(ioTask instanceof DataIoTask) {
			final DataIoTask dataIoTask = (DataIoTask) ioTask;
			respDataLatency = dataIoTask.getDataLatency();
			countBytesDone = dataIoTask.getCountBytesDone();
		} else {
			respDataLatency = 0;
			countBytesDone = 0;
		}
		
		/* perf trace logging
		if(!metricsConfig.getPrecondition()) {
			logTrace(
				ioTask.getLoadType(), nodeAddr, item, status, ioTask.getReqTimeStart(), reqDuration,
				respLatency, countBytesDone, respDataLatency
			);
		}*/
		
		if(IoTask.Status.SUCC == status) {
			// update the metrics with success
			if(respLatency > 0 && respLatency > reqDuration) {
				LOG.warn(
					Markers.ERR, "{}: latency {} is more than duration: {}", this, respLatency,
					reqDuration
				);
			}
			ioStats.markSucc(countBytesDone, reqDuration, respLatency);
			if(medIoStats != null && medIoStats.isStarted()) {
				medIoStats.markSucc(countBytesDone, reqDuration, respLatency);
			}
		} else {
			ioStats.markFail();
			if(medIoStats != null && medIoStats.isStarted()) {
				medIoStats.markFail();
			}
		}
	}

	@Override
	public int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to) {

		final int n;
		if(isInterrupted()) {
			n = 0;
		} else {
			n = to - from;
		}

		if(n > 0) {

			IoTask<I> ioTask;
			DataIoTask dataIoTask;
			I item;
			IoTask.Status status;
			String nodeAddr;
			int reqDuration, respLatency, respDataLatency = 0;
			long countBytesDone = 0;
			ioTask = ioTasks.get(from);
			final boolean isDataTransferred = ioTask instanceof DataIoTask;
			final boolean preconditionFlag = metricsConfig.getPrecondition();
			final LoadType ioType = ioTask.getLoadType();

			for(int i = from; i < to; i++) {
				if(i > from) {
					ioTask = ioTasks.get(i);
				}
				item = ioTask.getItem();
				status = ioTask.getStatus();
				nodeAddr = ioTask.getNodeAddr();
				reqDuration = ioTask.getDuration();
				respLatency = ioTask.getLatency();
				if(isDataTransferred) {
					dataIoTask = (DataIoTask) ioTask;
					respDataLatency = dataIoTask.getDataLatency();
					countBytesDone = dataIoTask.getCountBytesDone();
				}

				/* perf trace logging
				if(!preconditionFlag) {
					logTrace(
						ioType, nodeAddr, item, status, ioTask.getReqTimeStart(),
						reqDuration, respLatency, countBytesDone, respDataLatency
					);
				}*/

				if(IoTask.Status.SUCC == status) {
					// update the metrics with success
					if(respLatency > 0 && respLatency > reqDuration) {
						LOG.warn(
							Markers.ERR, "{}: latency {} is more than duration: {}", this, respLatency,
							reqDuration
						);
					}
					ioStats.markSucc(countBytesDone, reqDuration, respLatency);
					if(medIoStats != null && medIoStats.isStarted()) {
						medIoStats.markSucc(countBytesDone, reqDuration, respLatency);
					}
				} else {
					ioStats.markFail();
					if(medIoStats != null && medIoStats.isStarted()) {
						medIoStats.markFail();
					}
				}
			}
		}

		return n;
	}

	protected final static ThreadLocal<StringBuilder>
		PERF_TRACE_MSG_BUILDER = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	protected void logTrace(
		final LoadType ioType, final String nodeAddr, final I item, final IoTask.Status status,
		final long reqTimeStart, final long reqDuration, final int respLatency,
		final long countBytesDone, final int respDataLatency
	) {
		if(LOG.isInfoEnabled(Markers.PERF_TRACE)) {
			final StringBuilder strBuilder = PERF_TRACE_MSG_BUILDER.get();
			strBuilder.setLength(0);
			final String itemPath = item.getPath();
			LOG.info(
				Markers.PERF_TRACE,
				strBuilder
					.append(ioType).append(',')
					.append(nodeAddr == null ? "" : nodeAddr).append(',')
					.append(
						itemPath == null ?
						item.getName() :
						itemPath.endsWith(SLASH) ?
						itemPath + item.getName() :
						itemPath + SLASH + item.getName()
					)
					.append(',')
					.append(status.code).append(',')
					.append(reqTimeStart).append(',')
					.append(reqDuration).append(',')
					.append(respLatency).append(',')
					.append(countBytesDone).append(',')
					.append(respDataLatency)
					.toString()
			);
		}
	}

	@Override
	public final void registerDriver(final Driver<I, O> driver)
	throws IllegalStateException {
		if(null == drivers.putIfAbsent(driver.toString(), driver)) {
			LOG.info(
				Markers.MSG, "Monitor {}: driver {} registered", toString(), driver.toString()
			);
		} else {
			throw new IllegalStateException("Driver already registered");
		}
	}

	@Override
	public final IoStats.Snapshot getIoStatsSnapshot() {
		return ioStats.getSnapshot();
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	protected void doStart()
	throws UserShootHisFootException {
		for(final Generator<I, O> nextGenerator : generators) {
			nextGenerator.start();
		}
		if(ioStats != null) {
			ioStats.start();
		}
		worker.start();
	}

	@Override
	protected void doShutdown()
	throws UserShootHisFootException {
		for(final Generator<I, O> nextGenerator : generators) {
			nextGenerator.shutdown();
		}
	}

	@Override
	protected void doInterrupt()
	throws UserShootHisFootException {
		for(final Generator<I, O> nextGenerator : generators) {
			nextGenerator.interrupt();
		}
		worker.interrupt();
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return await(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		boolean allDriversFinished = true;
		Driver<I, O> nextDriver;
		do {
			for(final String driverName : drivers.keySet()) {
				nextDriver = drivers.get(driverName);
				if(!nextDriver.isInterrupted()) {
					allDriversFinished = false;
					break;
				}
				Thread.yield();
			}
		} while(!allDriversFinished);
		return false;
	}

	@Override
	public void close()
	throws IOException {
		if(!isInterrupted()) {
			try {
				interrupt();
			} catch(final UserShootHisFootException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to interrupt");
			}
		}
		generators.clear();
		drivers.clear();
		if(ioStats != null) {
			ioStats.close();
		}
		if(medIoStats != null) {
			medIoStats.close();
		}
	}
}
