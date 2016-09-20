package com.emc.mongoose.monitor;

import com.emc.mongoose.common.concurrent.InterruptibleDaemonBase;
import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.io.Output;
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
import java.util.concurrent.atomic.AtomicLong;
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
	private final int batchSize = 0x1000;
	private final ItemBuffer<I> itemOutBuff;
	private final IoStats ioStats, medIoStats;
	private volatile IoStats.Snapshot lastStats = null;
	private final Thread worker;
	private final AtomicLong counterResults = new AtomicLong(0);
	private volatile Output<I> itemOutput;
	
	public BasicMonitor(
		final String name, final List<Generator<I, O>> generators, final LoadConfig loadConfig
	) {
		this.name = name;
		this.generators = generators;
		for(final Generator<I, O> generator : generators) {
			generator.register(this);
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
		this.worker = new Thread(new ServiceTask(metricsPeriosSec), name);
		this.worker.setDaemon(true);
		final int maxItemQueueSize = loadConfig.getQueueConfig().getSize();
		this.itemOutBuff = new LimitedQueueItemBuffer<>(new ArrayBlockingQueue<>(maxItemQueueSize));
	}
	
	private final class ServiceTask
	implements Runnable {

		private final long metricsPeriodNanoSec;
		private long prevNanoTimeStamp;

		private ServiceTask(final int metricsPeriodSec) {
			this.metricsPeriodNanoSec = TimeUnit.SECONDS.toNanos(
				metricsPeriodSec > 0 ? metricsPeriodSec : Long.MAX_VALUE
			);
			this.prevNanoTimeStamp = -1;
		}

		@Override
		public final void run() {
			
			long nextNanoTimeStamp;
			
			while(!isInterrupted()) {
				nextNanoTimeStamp = System.nanoTime();
				// refresh the stats
				lastStats = ioStats.getSnapshot();
				// output the current measurements periodically
				if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
					LOG.info(Markers.PERF_AVG, lastStats.toString());
					prevNanoTimeStamp = nextNanoTimeStamp;
				}
				postProcessItems();
			}
		}
	}
	
	protected void postProcessItems() {
		final List<I> items = new ArrayList<>(batchSize);
		try {
			final int n = itemOutBuff.get(items, batchSize);
			if(n > 0) {
				if(itemOutput != null) {
					try {
						for(int m = 0; m < n; m += itemOutput.put(items, m, n)) {
							LockSupport.parkNanos(1);
						}
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", itemOutput
						);
					} finally {
						items.clear();
					}
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", itemOutput);
		} catch(final RejectedExecutionException e) {
			if(LOG.isTraceEnabled(Markers.ERR)) {
				LogUtil.exception(LOG, Level.TRACE, e, "\"{}\" rejected the items", itemOutput);
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
	private boolean isDone() {
		if(isDoneCountLimit()) {
			LOG.debug(Markers.MSG, "{}: done due to max count done state", getName());
			return true;
		}
		if(isDoneSizeLimit()) {
			LOG.debug(Markers.MSG, "{}: done due to max size done state", getName());
			return true;
		}
		return false;
	}
	
	
	@Override
	public void put(final O ioTask) {
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
			// put into the output buffer
			try {
				itemOutBuff.put(item);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e, "{}: failed to put the item into the output buffer",
					getName()
				);
			}
		} else {
			ioStats.markFail();
			if(medIoStats != null && medIoStats.isStarted()) {
				medIoStats.markFail();
			}
		}
		
		counterResults.incrementAndGet();
	}

	@Override
	public int put(final List<O> ioTasks, final int from, final int to) {

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
	
	@Override
	public int put(final List<O> buffer)
	throws IOException {
		return put(buffer, 0, buffer.size());
	}
	
	@Override
	public Input<O> getInput()
	throws IOException {
		return null;
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
	public final void register(final Driver<I, O> driver)
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
	public final void setItemOutput(final Output<I> itemOutput) {
		this.itemOutput = itemOutput;
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
