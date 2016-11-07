package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.DaemonBase;
import com.emc.mongoose.load.monitor.metrics.MetricsLogMessageCsv;
import com.emc.mongoose.load.monitor.metrics.MetricsLogMessageTable;
import com.emc.mongoose.model.io.Input;
import com.emc.mongoose.model.io.Output;
import com.emc.mongoose.model.item.ItemBuffer;
import com.emc.mongoose.model.item.LimitedQueueItemBuffer;
import com.emc.mongoose.load.monitor.metrics.BasicIoStats;
import com.emc.mongoose.model.load.LoadType;
import static com.emc.mongoose.ui.config.Config.LoadConfig.MetricsConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.model.io.task.DataIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.load.monitor.metrics.IoStats;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

/**
 Created by kurila on 12.07.16.
 */
public class BasicLoadMonitor<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadMonitor<I, O> {

	private static final Logger LOG = LogManager.getLogger();

	private final String name;
	private final Output<O> generatorOutput;
	private final List<LoadGenerator<I, O>> generators;
	private final List<StorageDriver<I, O>> drivers;
	private final MetricsConfig metricsConfig;
	private final long countLimit;
	private final long sizeLimit;
	private final int batchSize = 0x1000;
	private final ItemBuffer<I> itemOutBuff;
	private final Thread worker;

	private final Map<LoadType, IoStats>
		ioStats = new ConcurrentHashMap<>(),
		medIoStats = new ConcurrentHashMap<>();
	private volatile Map<LoadType, IoStats.Snapshot> lastStats = new ConcurrentHashMap<>();
	private final Function<LoadType, IoStats> ioStatsInitFunc;
	private final LongAdder counterResults = new LongAdder();
	private volatile Output<I> itemOutput;
	private volatile boolean isPostProcessDone = false;
	private final int totalConcurrency;
	
	public BasicLoadMonitor(
		final String name, final List<LoadGenerator<I, O>> generators,
		final List<StorageDriver<I,O>> drivers, final LoadConfig loadConfig
	) {
		this.name = name;
		final LimitConfig limitConfig = loadConfig.getLimitConfig();
		generatorOutput = new PendingIoTaskDispatcher<>(
			drivers, limitConfig.getRate(), null, null
		);
		this.generators = generators;
		for(final LoadGenerator<I, O> nextGenerator : generators) {
			nextGenerator.setOutput(generatorOutput);
		}
		this.drivers = drivers;
		int concurrencySum = 0;
		for(final StorageDriver<I, O> nextDriver : drivers) {
			try {
				concurrencySum += nextDriver.getConcurrencyLevel();
			} catch(final RemoteException ignored) {
			}
		}
		this.totalConcurrency = concurrencySum;
		registerDrivers(drivers);
		this.metricsConfig = loadConfig.getMetricsConfig();
		final int metricsPeriodSec = (int) metricsConfig.getPeriod();
		ioStatsInitFunc = ioType -> new BasicIoStats(ioType.toString(), metricsPeriodSec);
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
		this.worker = new Thread(new ServiceTask(metricsPeriodSec), name);
		this.worker.setDaemon(true);
		final int maxItemQueueSize = loadConfig.getQueueConfig().getSize();
		this.itemOutBuff = new LimitedQueueItemBuffer<>(new ArrayBlockingQueue<>(maxItemQueueSize));
		UNCLOSED.add(this);
	}

	protected void registerDrivers(final List<StorageDriver<I, O>> drivers) {
		for(final StorageDriver<I, O> nextDriver : drivers) {
			try {
				nextDriver.setOutput(this);
			} catch(final RemoteException ignored) {
			}
		}
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
			
			try {
				while(!worker.isInterrupted()) {
					nextNanoTimeStamp = System.nanoTime();
					// refresh the stats
					for(final LoadType nextIoType : ioStats.keySet()) {
						lastStats.put(nextIoType, ioStats.get(nextIoType).getSnapshot());
					}
					//
					postProcessItems();
					// output the current measurements periodically
					if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
						LOG.info(
							Markers.METRICS_STDOUT,
							new MetricsLogMessageTable(name, lastStats, totalConcurrency)
						);
						LOG.info(
							Markers.METRICS_FILE,
							new MetricsLogMessageCsv(lastStats, totalConcurrency)
						);
						prevNanoTimeStamp = nextNanoTimeStamp;
					}
					LockSupport.parkNanos(1_000_000);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted by concurrent modification");
			}
		}
	}
	
	private void postProcessItems()
	throws InterruptedException {
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
			} else if(isDone() || isIdle()) {
				isPostProcessDone = true;
			}
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", itemOutput
			);
		} catch(final RejectedExecutionException e) {
			if(LOG.isTraceEnabled(Markers.ERR)) {
				LogUtil.exception(LOG, Level.TRACE, e, "\"{}\" rejected the items", itemOutput);
			}
		} catch(final ConcurrentModificationException e) {
			throw new InterruptedException("Interrupt due to concurrent driver list modification");
		}
	}
	
	private boolean isDoneCountLimit() {
		if(countLimit > 0) {
			if(counterResults.sum() >= countLimit) {
				return true;
			}
			long succCountSum = 0;
			long failCountSum = 0;
			for(final LoadType ioType : lastStats.keySet()) {
				succCountSum += lastStats.get(ioType).getSuccCount();
				failCountSum += lastStats.get(ioType).getFailCount();
				if(succCountSum + failCountSum >= countLimit) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isDoneSizeLimit() {
		if(sizeLimit > 0) {
			long sizeSum = 0;
			for(final LoadType ioType : lastStats.keySet()) {
				sizeSum += lastStats.get(ioType).getByteCount();
				if(sizeSum >= sizeLimit) {
					return true;
				}
			}
		}
		return false;
	}

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

	private boolean isIdle()
	throws ConcurrentModificationException {

		boolean idleFlag = true;

		for(final LoadGenerator<I, O> nextLoadGenerator : generators) {
			try {
				if(!nextLoadGenerator.isInterrupted() && !nextLoadGenerator.isClosed()) {
					idleFlag = false;
					break;
				}
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to communicate with load generator \"{}\"",
					nextLoadGenerator
				);
			}
		}

		if(idleFlag) {
			for(final StorageDriver<I, O> nextStorageDriver : drivers) {
				try {
					if(
						!nextStorageDriver.isClosed() && !nextStorageDriver.isInterrupted() &&
						!nextStorageDriver.isIdle()
					) {
						idleFlag = false;
						break;
					}
				} catch(final NoSuchObjectException e) {
					if(!isClosed() && !isInterrupted()) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to communicate with storage driver \"{}\"",
							nextStorageDriver
						);
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to communicate with storage driver \"{}\"",
						nextStorageDriver
					);
				}
			}
		}

		return idleFlag;
	}
	
	
	@Override
	public void put(final O ioTask) {
		if(isInterrupted() || isClosed()) {
			return;
		}
		final I item = ioTask.getItem();
		final LoadType ioType = ioTask.getLoadType();
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
				ioType, nodeAddr, item, status, ioTask.getReqTimeStart(), reqDuration,
				respLatency, countBytesDone, respDataLatency
			);
		}
		
		final IoStats loadTypeStats = ioStats.computeIfAbsent(ioType, ioStatsInitFunc);
		final IoStats loadTypeMedStats = medIoStats.computeIfAbsent(ioType, ioStatsInitFunc);
		
		if(IoTask.Status.SUCC == status) {
			// update the metrics with success
			loadTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
			if(loadTypeMedStats != null && loadTypeMedStats.isStarted()) {
				loadTypeMedStats.markSucc(countBytesDone, reqDuration, respLatency);
			}
			// TODO set the destination item path
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
			loadTypeStats.markFail();
			if(loadTypeMedStats != null && loadTypeMedStats.isStarted()) {
				loadTypeMedStats.markFail();
			}
		}
		
		counterResults.increment();
	}

	@Override
	public int put(final List<O> ioTasks, final int from, final int to) {

		final int n;
		if(isInterrupted() || isClosed()) {
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
			int reqDuration, respLatency, respDataLatency = -1;
			long countBytesDone = 0;
			ioTask = ioTasks.get(from);
			final boolean isDataTransferred = ioTask instanceof DataIoTask;
			final boolean preconditionFlag = metricsConfig.getPrecondition();
			LoadType ioType;
			IoStats loadTypeStats, loadTypeMedStats;

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
				ioType = ioTask.getLoadType();
				if(!preconditionFlag) {
					logTrace(
						ioType, nodeAddr, item, status, ioTask.getReqTimeStart(),
						reqDuration, respLatency, countBytesDone, respDataLatency
					);
				}
				
				loadTypeStats = ioStats.computeIfAbsent(ioType, ioStatsInitFunc);
				loadTypeMedStats = medIoStats.computeIfAbsent(ioType, ioStatsInitFunc);
				
				if(IoTask.Status.SUCC == status) {
					// update the metrics with success
					if(respLatency > 0 && respLatency > reqDuration) {
						LOG.warn(
							Markers.ERR, "{}: latency {} is more than duration: {}", this, respLatency,
							reqDuration
						);
					}
					loadTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
					if(loadTypeMedStats != null && loadTypeMedStats.isStarted()) {
						loadTypeMedStats.markSucc(countBytesDone, reqDuration, respLatency);
					}
					// TODO set the destination item path
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
					loadTypeStats.markFail();
					if(loadTypeMedStats != null && loadTypeMedStats.isStarted()) {
						loadTypeMedStats.markFail();
					}
				}
			}

			counterResults.add(n);
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
	
	private static final ThreadLocal<StringBuilder>
		PERF_TRACE_MSG_BUILDER = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};
	private void logTrace(
		final LoadType ioType, final String nodeAddr, final I item, final IoTask.Status status,
		final long reqTimeStart, final long reqDuration, final int respLatency,
		final long countBytesDone, final int respDataLatency
	) {
		if(LOG.isInfoEnabled(Markers.IO_TRACE)) {
			final StringBuilder strBuilder = PERF_TRACE_MSG_BUILDER.get();
			strBuilder.setLength(0);
			LOG.info(
				Markers.IO_TRACE,
				strBuilder
					.append(ioType).append(',')
					.append(nodeAddr == null ? "" : nodeAddr).append(',')
					.append(item.getName())
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
	public final String getName() {
		return name;
	}
	
	@Override
	public final void setItemOutput(final Output<I> itemOutput) {
		this.itemOutput = itemOutput;
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		for(final StorageDriver<I, O> nextDriver : drivers) {
			try {
				nextDriver.start();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to start the driver {}", nextDriver.toString()
				);
			}
		}
		for(final LoadGenerator<I, O> nextGenerator : generators) {
			try {
				nextGenerator.start();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to start the generator {}", nextGenerator.toString()
				);
			}
		}
		if(ioStats != null) {
			for(final LoadType ioType : ioStats.keySet()) {
				ioStats.get(ioType).start();
			}
		}
		worker.start();
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {
		for(final LoadGenerator<I, O> nextGenerator : generators) {
			try {
				nextGenerator.interrupt();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to interrupt the generator {}",
					nextGenerator.toString()
				);
			}
		}
		for(final StorageDriver<I, O> nextDriver : drivers) {
			try {
				nextDriver.shutdown();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to shutdown the driver {}",
					nextDriver.toString()
				);
			}
		}
	}

	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		for(final StorageDriver<I, O> nextDriver : drivers) {
			try {
				nextDriver.interrupt();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e, "Failed to interrupt the driver {}",
					nextDriver.toString()
				);
			}
		}
		worker.interrupt();
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		long t, timeOutMilliSec = timeUnit.toMillis(timeout);
		/*if(loadedPrevState != null) {
			if(isLimitReached) {
				return true;
			}
			t = TimeUnit.MICROSECONDS.toMillis(
				loadedPrevState.getStatsSnapshot().getElapsedTime()
			);
			timeOutMilliSec -= t;
		}*/
		//
		LOG.debug(
			Markers.MSG, "{}: await for the done condition at most for {}[s]",
			getName(), TimeUnit.NANOSECONDS.toSeconds(timeOutMilliSec)
		);
		t = System.currentTimeMillis();
		while(System.currentTimeMillis() - t < timeOutMilliSec) {
			synchronized(state) {
				state.wait(100);
			}
			if(isInterrupted()) {
				LOG.debug(Markers.MSG, "{}: await exit due to \"interrupted\" state", getName());
				return true;
			}
			if(isClosed()) {
				LOG.debug(Markers.MSG, "{}: await exit due to \"closed\" state", getName());
				return true;
			}
			if(isPostProcessDone) {
				LOG.debug(Markers.MSG, "{}: await exit due to \"done\" state", getName());
				return true;
			}
		}
		LOG.debug(Markers.MSG, "{}: await exit due to timeout", getName());
		return false;
	}

	@Override
	protected void doClose()
	throws IOException {

		for(final LoadGenerator<I, O> generator : generators) {
			try {
				generator.close();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to close the generator {}", generator
				);
			}
		}
		generators.clear();

		for(final StorageDriver<I, O> driver : drivers) {
			try {
				driver.close();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to close the driver {}", driver);
			}
		}
		drivers.clear();

		LOG.info(
			Markers.METRICS_STDOUT, new MetricsLogMessageTable(name, lastStats, totalConcurrency)
		);
		LOG.info(
			Markers.METRICS_FILE_TOTAL, new MetricsLogMessageCsv(lastStats, totalConcurrency)
		);
		
		for(final IoStats nextStats : ioStats.values()) {
			nextStats.close();
		}
		ioStats.clear();
		
		if(medIoStats != null) {
			for(final IoStats nextMedStats : medIoStats.values()) {
				nextMedStats.close();
			}
			medIoStats.clear();
		}
		
		if(itemOutput != null) {
			itemOutput.close();
		}
		itemOutBuff.close();
		UNCLOSED.remove(this);
	}
}
