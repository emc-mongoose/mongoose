package com.emc.mongoose.monitor;

import com.emc.mongoose.common.concurrent.InterruptibleDaemonBase;
import com.emc.mongoose.model.impl.metrics.BasicIoStats;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import static com.emc.mongoose.ui.config.Config.LoadConfig.MetricsConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.model.api.item.Item.SLASH;

/**
 Created by kurila on 12.07.16.
 */
public class BasicMonitor<I extends Item, O extends IoTask<I>>
extends InterruptibleDaemonBase
implements Monitor<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	private final static ScheduledThreadPoolExecutor
		LOG_METRICS_SERVICE = new ScheduledThreadPoolExecutor(1);

	private final String name;
	private final List<Generator<I, O>> generators;
	private final ConcurrentMap<String, Driver<I, O>> drivers = new ConcurrentHashMap<>();
	private final MetricsConfig metricsConfig;
	private final LimitConfig limitConfig;
	private final IoStats ioStats, medIoStats;

	private final static class LogMetricsTask
	implements Runnable {

		private final Monitor monitor;

		public LogMetricsTask(final Monitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public void run() {
			Thread.currentThread().setName(monitor.getName());
			LOG.info(Markers.PERF_AVG, monitor.getIoStatsSnapshot().toString());
		}
	}
	private final LogMetricsTask logMetricsTask;

	public BasicMonitor(
		final String name, final List<Generator<I, O>> generators,
		final MetricsConfig metricsConfig, final LimitConfig limitConfig
	) {
		this.name = name;
		this.generators = generators;
		for(final Generator<I, O> generator : generators) {
			generator.registerMonitor(this);
		}
		this.metricsConfig = metricsConfig;
		final int metricsPeriosSec = (int) metricsConfig.getPeriod();
		this.ioStats = new BasicIoStats(name, metricsPeriosSec);
		this.medIoStats = new BasicIoStats(name, metricsPeriosSec);
		this.logMetricsTask = new LogMetricsTask(this);
		this.limitConfig = limitConfig;
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
		LOG_METRICS_SERVICE.scheduleAtFixedRate(
			logMetricsTask, 0, metricsConfig.getPeriod(), TimeUnit.SECONDS
		);
		if(ioStats != null) {
			ioStats.start();
		}
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
		LOG_METRICS_SERVICE.getQueue().remove(logMetricsTask);
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
