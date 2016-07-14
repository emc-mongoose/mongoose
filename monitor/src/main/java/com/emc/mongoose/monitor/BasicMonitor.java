package com.emc.mongoose.monitor;

import com.emc.mongoose.common.concurrent.LifeCycleBase;
import com.emc.mongoose.common.config.Config;
import com.emc.mongoose.common.config.LoadType;
import com.emc.mongoose.common.io.DataIoTask;
import com.emc.mongoose.common.io.IoTask;
import com.emc.mongoose.common.item.Item;
import com.emc.mongoose.common.load.Driver;
import com.emc.mongoose.common.load.Generator;
import com.emc.mongoose.common.load.Monitor;
import com.emc.mongoose.common.load.metrics.IoStats;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.emc.mongoose.common.item.Item.SLASH;
/**
 Created by kurila on 12.07.16.
 */
public class BasicMonitor<I extends Item, O extends IoTask<I>>
extends LifeCycleBase
implements Monitor<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	private final static ScheduledThreadPoolExecutor
		LOG_METRICS_SERVICE = new ScheduledThreadPoolExecutor(1);

	private final List<Generator<I, O>> generators;
	private final ConcurrentMap<String, Driver<I, O>> drivers = new ConcurrentHashMap<>();
	private final Config.LoadConfig.MetricsConfig metricsConfig;

	private final static class LogMetricsTask
	implements Runnable {

		private final Monitor monitor;

		public LogMetricsTask(final Monitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public void run() {
			LOG.info(Markers.PERF_AVG, monitor.getIoStatsSnapshot().toString());
		}
	}
	private final LogMetricsTask logMetricsTask;

	public BasicMonitor(
		final List<Generator<I, O>> generators, final Config.LoadConfig.MetricsConfig metricsConfig
	) {
		this.generators = generators;
		for(final Generator<I, O> generator : generators) {
			generator.registerMonitor(this);
		}
		this.metricsConfig = metricsConfig;
		this.logMetricsTask = new LogMetricsTask(this);
	}

	private final AtomicLong taskCounter = new AtomicLong(0);

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
		// perf trace logging
		if(!metricsConfig.getPrecondition()) {
			logTrace(
				ioTask.getLoadType(), nodeAddr, item, status, ioTask.getReqTimeStart(), reqDuration,
				respLatency, countBytesDone, respDataLatency
			);
		}
		// TODO statistics accounting
		taskCounter.incrementAndGet();
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
				// perf trace logging
				if(!preconditionFlag) {
					logTrace(
						ioType, nodeAddr, item, status, ioTask.getReqTimeStart(),
						reqDuration, respLatency, countBytesDone, respDataLatency
					);
				}
			}

			// TODO batch statistics accounting
			taskCounter.addAndGet(n);
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
		return null;
	}

	@Override
	protected void doStart() {
		for(final Generator<I, O> nextGenerator : generators) {
			nextGenerator.start();
		}
		LOG_METRICS_SERVICE.scheduleAtFixedRate(
			logMetricsTask, 0, metricsConfig.getPeriod(), TimeUnit.SECONDS
		);
	}

	@Override
	protected void doShutdown() {
		for(final Generator<I, O> nextGenerator : generators) {
			nextGenerator.shutdown();
		}
	}

	@Override
	protected void doInterrupt() {
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
			interrupt();
		}
		generators.clear();
		drivers.clear();
	}
}
