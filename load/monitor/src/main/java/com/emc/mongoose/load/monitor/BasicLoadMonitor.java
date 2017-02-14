package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.concurrent.RateThrottle;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.concurrent.WeightThrottle;
import com.emc.mongoose.load.monitor.metrics.MetricsSvcTask;
import com.emc.mongoose.model.DaemonBase;
import com.emc.mongoose.model.io.task.IoTask.Status;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.io.task.path.PathIoTask;
import com.emc.mongoose.ui.log.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.load.monitor.metrics.ExtResultsXmlLogMessage;
import com.emc.mongoose.load.monitor.metrics.IoTraceCsvBatchLogMessage;
import com.emc.mongoose.load.monitor.metrics.MetricsCsvLogMessage;
import com.emc.mongoose.load.monitor.metrics.MetricsStdoutLogMessage;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.io.collection.RoundRobinOutput;
import com.emc.mongoose.load.monitor.metrics.BasicIoStats;
import static com.emc.mongoose.ui.config.Config.LoadConfig.MetricsConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.model.io.IoType;
import static java.lang.System.nanoTime;

import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.load.monitor.metrics.IoStats;
import com.emc.mongoose.ui.log.Markers;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 12.07.16.
 */
public class BasicLoadMonitor<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadMonitor<I, O> {

	private static final Logger LOG = LogManager.getLogger();

	private final String name;
	private final Map<LoadGenerator<I, O>, List<StorageDriver<I, O>>> driversMap;
	private final boolean preconditionJobFlag;
	private final int metricsPeriodSec;
	private final int totalConcurrency;
	private final double fullLoadThreshold;
	private final long countLimit;
	private final long sizeLimit;
	private final ConcurrentMap<I, O> latestIoResultsPerItem;
	private final boolean isAnyCircular;
	private final ThreadPoolExecutor svcTaskExecutor;

	private final Int2ObjectMap<IoStats> ioStats = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectMap<IoStats> medIoStats;
	private final Int2ObjectMap<IoStats.Snapshot> lastStats = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectMap<IoStats.Snapshot> lastMedStats;
	private final Int2ObjectMap<SizeInBytes> itemSizeMap = new Int2ObjectOpenHashMap<>();
	private final LongAdder counterResults = new LongAdder();
	private volatile Output<O> ioResultsOutput;
	private final Int2IntMap concurrencyMap;
	private final Int2IntMap driversCountMap;
	private final Int2BooleanMap circularityMap;
	private final Throttle<Object> rateThrottle;
	private final WeightThrottle weightThrottle;
	private final Int2ObjectMap<Output<O>> ioTaskOutputs = new Int2ObjectOpenHashMap<>();

	/**
	 Single load job constructor
	 @param name
	 @param loadGenerator
	 @param driversMap
	 @param loadConfig
	 */
	public BasicLoadMonitor(
		final String name, final LoadGenerator<I, O> loadGenerator,
		final List<StorageDriver<I, O>> driversMap, final LoadConfig loadConfig
	) {
		this(
			name,
			new HashMap<LoadGenerator<I, O>, List<StorageDriver<I, O>>>() {{
				put(loadGenerator, driversMap);
			}},
			new HashMap<LoadGenerator<I, O>, LoadConfig>() {{
				put(loadGenerator, loadConfig);
			}},
			null
		);
	}

	/**
	 Mixed load job constructor
	 @param name
	 @param driversMap
	 @param loadConfigs
	 */
	public BasicLoadMonitor(
		final String name,
		final Map<LoadGenerator<I, O>, List<StorageDriver<I, O>>> driversMap,
		final Map<LoadGenerator<I, O>, LoadConfig> loadConfigs
	) {
		this(name, driversMap, loadConfigs, null);
	}

	/**
	 Weighted mixed load job constructor
	 @param name
	 @param driversMap
	 @param loadConfigs
	 @param weightMap
	 */
	public BasicLoadMonitor(
		final String name,
		final Map<LoadGenerator<I, O>, List<StorageDriver<I, O>>> driversMap,
		final Map<LoadGenerator<I, O>, LoadConfig> loadConfigs, final Int2IntMap weightMap
	) {
		this.name = name;

		final LoadConfig firstLoadConfig = loadConfigs.get(loadConfigs.keySet().iterator().next());
		final double rateLimit = firstLoadConfig.getLimitConfig().getRate();
		if(rateLimit > 0) {
			rateThrottle = new RateThrottle<>(rateLimit);
		} else {
			rateThrottle = null;
		}
		
		if(weightMap == null || weightMap.size() == 0 || weightMap.size() == 1) {
			weightThrottle = null;
		} else {
			weightThrottle = new WeightThrottle(weightMap);
		}

		Output<O> nextGeneratorOutput;
		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			nextGeneratorOutput = new RoundRobinOutput<>(driversMap.get(nextGenerator));
			ioTaskOutputs.put(nextGenerator.hashCode(), nextGeneratorOutput);
			nextGenerator.setWeightThrottle(weightThrottle);
			nextGenerator.setRateThrottle(rateThrottle);
			nextGenerator.setOutput(nextGeneratorOutput);
		}

		final MetricsConfig metricsConfig = firstLoadConfig.getMetricsConfig();
		preconditionJobFlag = metricsConfig.getPrecondition();
		metricsPeriodSec = (int) metricsConfig.getPeriod();
		fullLoadThreshold = metricsConfig.getThreshold();
		if(fullLoadThreshold > 0) {
			medIoStats = new Int2ObjectOpenHashMap<>();
			lastMedStats = new Int2ObjectOpenHashMap<>();
		} else {
			medIoStats = null;
			lastMedStats = null;
		}

		this.driversMap = driversMap;
		concurrencyMap = new Int2IntOpenHashMap(driversMap.size());
		driversCountMap = new Int2IntOpenHashMap(driversMap.size());
		circularityMap = new Int2BooleanArrayMap(driversMap.size());
		int concurrencySum = 0;
		int driversCountSum = 0;
		boolean anyCircularFlag = false;
		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O>> nextDrivers = driversMap.get(nextGenerator);
			driversCountSum += nextDrivers.size();
			final LoadConfig nextLoadConfig = loadConfigs.get(nextGenerator);
			circularityMap.put(nextGenerator.hashCode(), nextLoadConfig.getCircular());
			if(circularityMap.get(nextGenerator.hashCode())) {
				anyCircularFlag = true;
			}
			final String ioTypeName = nextLoadConfig.getType().toUpperCase();
			final int ioTypeCode = IoType.valueOf(ioTypeName).ordinal();
			driversCountMap.put(ioTypeCode, nextDrivers.size());
			concurrencySum += loadConfigs.get(nextGenerator).getConcurrency();
			concurrencyMap.put(ioTypeCode, loadConfigs.get(nextGenerator).getConcurrency());
			ioStats.put(
				ioTypeCode, new BasicIoStats(IoType.values()[ioTypeCode].name(), metricsPeriodSec)
			);
			if(medIoStats != null) {
				medIoStats.put(
					ioTypeCode,
					new BasicIoStats(IoType.values()[ioTypeCode].name(), metricsPeriodSec)
				);
			}
			itemSizeMap.put(nextGenerator.getIoType().ordinal(), nextGenerator.getItemSizeEstimate());
		}
		this.totalConcurrency = concurrencySum;
		this.isAnyCircular = anyCircularFlag;
		if(isAnyCircular) {
			latestIoResultsPerItem = new ConcurrentHashMap<>(firstLoadConfig.getQueueConfig().getSize());
		} else {
			latestIoResultsPerItem = null;
		}

		long countLimitSum = 0;
		long sizeLimitSum = 0;
		for(final LoadGenerator<I, O> nextLoadGenerator : loadConfigs.keySet()) {
			final LimitConfig nextLimitConfig = loadConfigs.get(nextLoadGenerator).getLimitConfig();
			if(nextLimitConfig.getCount() > 0 && countLimitSum < Long.MAX_VALUE) {
				countLimitSum += nextLimitConfig.getCount();
			} else {
				countLimitSum = Long.MAX_VALUE;
			}
			if(nextLimitConfig.getSize().get() > 0 && sizeLimitSum < Long.MAX_VALUE) {
				sizeLimitSum += nextLimitConfig.getSize().get();
			} else {
				sizeLimitSum = Long.MAX_VALUE;
			}
		}
		this.countLimit = countLimitSum;
		this.sizeLimit = sizeLimitSum;

		final int svcWorkerCount = driversCountSum + (medIoStats == null ? 1 : 2);
		this.svcTaskExecutor = new ThreadPoolExecutor(
			svcWorkerCount, svcWorkerCount, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
			new NamingThreadFactory("svcTasksExecutor", true)
		);

		UNCLOSED.add(this);
	}

	private boolean isDoneCountLimit() {
		if(countLimit > 0) {
			if(counterResults.sum() >= countLimit) {
				return true;
			}
			long succCountSum = 0;
			long failCountSum = 0;
			for(final int ioTypeCode : lastStats.keySet()) {
				succCountSum += lastStats.get(ioTypeCode).getSuccCount();
				failCountSum += lastStats.get(ioTypeCode).getFailCount();
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
			for(final int ioTypeCode : lastStats.keySet()) {
				sizeSum += lastStats.get(ioTypeCode).getByteCount();
				if(sizeSum >= sizeLimit) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean allIoTasksCompleted() {
		long generatedIoTasks = 0;
		for(final LoadGenerator<I, O> nextLoadGenerator : driversMap.keySet()) {
			try {
				if(nextLoadGenerator.isInterrupted()) {
					generatedIoTasks += nextLoadGenerator.getGeneratedIoTasksCount();
				} else {
					return false;
				}
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to communicate with load generator \"{}\"",
					nextLoadGenerator
				);
			}
		}
		return counterResults.longValue() >= generatedIoTasks;
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

		for(final LoadGenerator<I, O> nextLoadGenerator : driversMap.keySet()) {

			try {
				if(!nextLoadGenerator.isInterrupted() && !nextLoadGenerator.isClosed()) {
					return false;
				}
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to communicate with load generator \"{}\"",
					nextLoadGenerator
				);
			}

			for(final StorageDriver<I, O> nextStorageDriver : driversMap.get(nextLoadGenerator)) {
				try {
					if(
						!nextStorageDriver.isClosed() && !nextStorageDriver.isInterrupted() &&
						!nextStorageDriver.isIdle()
					) {
						return false;
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

		return true;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final void setIoResultsOutput(final Output<O> ioTaskResultsOutput) {
		this.ioResultsOutput = ioTaskResultsOutput;
	}

	@Override
	public final void processIoResults(final List<O> ioTaskResults, final int n) {

		int m = n; // count of complete whole tasks

		// I/O trace logging
		if(!preconditionJobFlag && LOG.isDebugEnabled(Markers.IO_TRACE)) {
			LOG.debug(Markers.IO_TRACE, new IoTraceCsvBatchLogMessage<>(ioTaskResults, 0, n));
		}

		int originCode;
		I item;
		O ioTaskResult;
		int ioTypeCode;
		Status status;
		long reqDuration;
		long respLatency;
		long countBytesDone = 0;
		ioTaskResult = ioTaskResults.get(0);
		IoStats ioTypeStats, ioTypeMedStats;
		Output<O> ioTaskDest;

		try {
			for(int i = 0; i < n; i ++) {
				
				if(i > 0) {
					ioTaskResult = ioTaskResults.get(i);
				}
				
				if( // account only completed composite I/O tasks
					ioTaskResult instanceof CompositeIoTask &&
					!((CompositeIoTask) ioTaskResult).allSubTasksDone()
				) {
					m --;
					continue;
				}
				
				originCode = ioTaskResult.getOriginCode();
				ioTypeCode = ioTaskResult.getIoType().ordinal();
				status = ioTaskResult.getStatus();
				reqDuration = ioTaskResult.getRespTimeDone() - ioTaskResult.getReqTimeStart();
				respLatency = ioTaskResult.getRespTimeStart() - ioTaskResult.getReqTimeDone();
				if(ioTaskResult instanceof DataIoTask) {
					countBytesDone = ((DataIoTask) ioTaskResult).getCountBytesDone();
				} else if(ioTaskResult instanceof PathIoTask) {
					countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
				}
				
				ioTypeStats = ioStats.get(ioTypeCode);
				ioTypeMedStats = medIoStats == null ? null : medIoStats.get(ioTypeCode);
				
				if(Status.SUCC.equals(status)) {
					if(respLatency > 0 && respLatency > reqDuration) {
						LOG.debug(Markers.ERR, "Dropping invalid latency value {}", respLatency);
					}
					if(ioTaskResult instanceof PartialIoTask) {
						ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
						if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
							ioTypeMedStats.markPartSucc(countBytesDone, reqDuration, respLatency);
						}
						m --;
					} else {
						
						if(circularityMap.get(originCode)) {
							item = ioTaskResult.getItem();
							latestIoResultsPerItem.put(item, ioTaskResult);
							if(rateThrottle != null) {
								while(!rateThrottle.tryAcquire(ioTaskResult)) {
									LockSupport.parkNanos(1);
								}
							}
							if(weightThrottle != null) {
								while(!weightThrottle.tryAcquire(originCode)) {
									LockSupport.parkNanos(1);
								}
							}
							ioTaskDest = ioTaskOutputs.get(originCode);
						} else {
							ioTaskDest = ioResultsOutput;
						}
						
						if(ioTaskDest != null) {
							while(!ioTaskDest.put(ioTaskResult)) {
								LockSupport.parkNanos(1);
							}
						}
						
						// update the metrics with success
						ioTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
						if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
							ioTypeMedStats.markSucc(countBytesDone, reqDuration, respLatency);
						}
					}
				} else if(!Status.CANCELLED.equals(status)) {
					LOG.debug(Markers.ERR, ioTaskResult.toString());
					ioTypeStats.markFail();
					if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
						ioTypeMedStats.markFail();
					}
				}
			}
		} catch(final EOFException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "I/O task destination end of input");
		} catch(final NoSuchObjectException e) {
			LogUtil.exception(
				LOG, Level.DEBUG, e, "Remote I/O task destination is not more available"
			);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to process the I/O task results");
		}

		counterResults.add(m);
	}
	
	@Override
	public final int getActiveTaskCount() {
		int totalActiveTaskCount = 0;
		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O>> nextGeneratorDrivers = driversMap.get(nextGenerator);
			for(final StorageDriver<I, O> nextDriver : nextGeneratorDrivers) {
				try {
					totalActiveTaskCount += nextDriver.getActiveTaskCount();
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to invoke the remote method");
				}
			}
		}
		return totalActiveTaskCount;
	}
	
	private final class IntermediateMetricsSvcTask
	implements Runnable {
		
		@Override
		public final void run() {
			
			if(medIoStats == null) {
				return;
			}
			final Thread currThread = Thread.currentThread();
			currThread.setName(name + "-intermediate-metrics");
			final int activeTaskCountThreshold = (int) (fullLoadThreshold * totalConcurrency);
			
			try {
				while(getActiveTaskCount() < activeTaskCountThreshold) {
					Thread.sleep(1);
				}
				LOG.info(
					Markers.MSG,
					"The threshold of {} active tasks count is reached, starting the additional metrics accounting",
					activeTaskCountThreshold
				);
				for(final IoStats nextMedIoStats : medIoStats.values()) {
					nextMedIoStats.start();
				}
				
				final long metricsPeriodNanoSec = TimeUnit.SECONDS.toNanos(
					metricsPeriodSec > 0 ? metricsPeriodSec : Long.MAX_VALUE
				);
				long prevNanoTimeStamp = -1, nextNanoTimeStamp;
				while(getActiveTaskCount() >= activeTaskCountThreshold) {
					IoStats.refreshLastStats(medIoStats, lastMedStats);
					nextNanoTimeStamp = nanoTime();
					if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
						IoStats.outputLastMedStats(
							lastMedStats, driversCountMap, concurrencyMap, name, preconditionJobFlag
						);
						prevNanoTimeStamp = nextNanoTimeStamp;
					}
					Thread.sleep(1);
				}
				LOG.info(
					Markers.MSG,
					"The active tasks count is below the threshold of {}, stopping the additional metrics accounting",
					activeTaskCountThreshold
				);
			} catch(final InterruptedException ignored) {
			} finally {
			
				LOG.info(
					Markers.METRICS_MED_STDOUT,
					new MetricsStdoutLogMessage(name, lastMedStats, concurrencyMap, driversCountMap)
				);
				if(!preconditionJobFlag) {
					LOG.info(
						Markers.METRICS_MED_FILE_TOTAL,
						new MetricsCsvLogMessage(lastMedStats, concurrencyMap, driversCountMap)
					);
					LOG.info(
						Markers.METRICS_EXT_MED_RESULTS,
						new ExtResultsXmlLogMessage(
							name, lastStats, itemSizeMap, concurrencyMap, driversCountMap
						)
					);
				}
				
				for(final IoStats nextMedIoStats : medIoStats.values()) {
					try {
						nextMedIoStats.close();
					} catch(final IOException e) {
						LogUtil.exception(LOG, Level.WARN, e, "Unexpected failure");
					}
				}
				medIoStats.clear();
			}
		}
	}
	
	@Override
	protected void doStart()
	throws IllegalStateException {

		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O>> nextGeneratorDrivers = driversMap.get(nextGenerator);
			for(final StorageDriver<I, O> nextDriver : nextGeneratorDrivers) {
				try {
					nextDriver.start();
				} catch(final IllegalStateException | RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to start the driver {}", nextDriver.toString()
					);
				}
			}
			try {
				nextGenerator.start();
			} catch(final IllegalStateException | RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to start the generator {}", nextGenerator.toString()
				);
			}
		}

		for(final int ioTypeCode : concurrencyMap.keySet()) {
			ioStats.get(ioTypeCode).start();
		}

		svcTaskExecutor.submit(
			new MetricsSvcTask(
				name, metricsPeriodSec, preconditionJobFlag, ioStats, lastStats,
				driversCountMap, concurrencyMap
			)
		);
		if(medIoStats != null) {
			svcTaskExecutor.submit(new IntermediateMetricsSvcTask());
		}
		for(final LoadGenerator<I, O> generator : driversMap.keySet()) {
			for(final StorageDriver<I, O> driver : driversMap.get(generator)) {
				svcTaskExecutor.submit(
					new GetAndProcessIoResultsSvcTask<>(this, driver)
				);
			}
		}
		svcTaskExecutor.shutdown();
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {
		
		final ExecutorService shutdownExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareConcurrencyLevel(),
			new NamingThreadFactory("shutdownWorker", true)
		);

		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			shutdownExecutor.submit(
				() -> {
					try {
						nextGenerator.interrupt();
						LOG.debug(
							Markers.MSG, "{}: load generator \"{}\" shut down", getName(),
							nextGenerator.toString()
						);
					} catch(final RemoteException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "{}: failed to interrupt the generator {}",
							getName(), nextGenerator.toString()
						);
					}
				}
			);
			for(final StorageDriver<I, O> nextDriver : driversMap.get(nextGenerator)) {
				shutdownExecutor.submit(
					() -> {
						try {
							nextDriver.shutdown();
							LOG.debug(
								Markers.MSG, "{}: storage driver \"{}\" shut down", getName(),
								nextDriver.toString()
							);
						} catch(final RemoteException e) {
							LogUtil.exception(
								LOG, Level.WARN, e, "failed to shutdown the driver {}", getName(),
								nextDriver.toString()
							);
						}
					}
				);
			}
		}
		
		shutdownExecutor.shutdown();
		try {
			if(shutdownExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				LOG.debug(Markers.MSG, "{}: load monitor was shut down properly", getName());
			} else {
				LOG.warn(Markers.ERR, "{}: load monitor shutdown timeout", getName());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "{}: load monitor shutdown interrupted", getName()
			);
		}
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
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
			getName(), TimeUnit.MILLISECONDS.toSeconds(timeOutMilliSec)
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
			if(isDone()) {
				LOG.debug(Markers.MSG, "{}: await exit due to \"done\" state", getName());
				return true;
			}
			if(!isAnyCircular && allIoTasksCompleted()) {
				LOG.debug(
					Markers.MSG, "{}: await exit because all I/O tasks have been completed", getName()
				);
				return true;
			}
		}
		LOG.debug(Markers.MSG, "{}: await exit due to timeout", getName());
		return false;
	}

	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		
		final ExecutorService interruptExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareConcurrencyLevel(),
			new NamingThreadFactory("interruptWorker", true)
		);

		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			for(final StorageDriver<I, O> nextDriver : driversMap.get(nextGenerator)) {
				interruptExecutor.submit(
					() -> {
						try {
							nextDriver.interrupt();
						} catch(final RemoteException e) {
							LogUtil.exception(
								LOG, Level.DEBUG, e, "{}: failed to interrupt the driver {}",
								getName(), nextDriver.toString()
							);
						}
					}
				);
			}
		}
		
		interruptExecutor.shutdown();
		try {
			if(interruptExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				LOG.debug(
					Markers.MSG, "{}: storage drivers have been interrupted properly", getName()
				);
			} else {
				LOG.warn(Markers.ERR, "{}: storage drivers interrupting timeout", getName());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "{}: storage drivers interrupting interrupted", getName()
			);
		}

		svcTaskExecutor.shutdownNow();
		try {
			if(!svcTaskExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				LOG.error(
					Markers.ERR, "{}: failed to terminate the service tasks in 1 second", getName()
				);
			}
		} catch(final InterruptedException e) {
			throw new AssertionError(e);
		}
		LOG.debug(Markers.MSG, "{}: interrupted the load monitor", getName());
	}

	@Override
	protected final void doClose()
	throws IOException {

		final ExecutorService ioResultsGetAndApplyExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareConcurrencyLevel(),
			new NamingThreadFactory("ioResultsGetAndApplyWorker", true)
		);

		for(final LoadGenerator<I, O> generator : driversMap.keySet()) {

			for(final StorageDriver<I, O> driver : driversMap.get(generator)) {
				
				ioResultsGetAndApplyExecutor.submit(
					() -> {
						try {
							final List<O> finalResults = driver.getResults();
							if(finalResults != null) {
								final int finalResultsCount = finalResults.size();
								if(finalResultsCount > 0) {
									LOG.debug(
										Markers.MSG,
										"{}: the driver \"{}\" returned {} final I/O results to process",
										getName(), driver.toString(), finalResults.size()
									);
									processIoResults(finalResults, finalResultsCount);
								}
							}
						} catch(final Throwable cause) {
							LogUtil.exception(
								LOG, Level.WARN, cause,
								"{}: failed to process the final results for the driver {}",
								getName(), driver.toString()
							);
						}
		
						try {
							driver.close();
							LOG.debug(
								Markers.MSG, "{}: the storage driver \"{}\" has been closed",
								getName(), driver.toString()
							);
						} catch(final IOException e) {
							LogUtil.exception(
								LOG, Level.WARN, e, "{}: failed to close the driver {}", getName(),
								driver.toString()
							);
						}
					}
				);
			}

			try {
				generator.close();
				LOG.debug(
					Markers.MSG, "{}: the load generator \"{}\" has been closed", getName(),
					generator
				);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "{}: failed to close the generator {}", getName(), generator
				);
			}
		}
		
		ioResultsGetAndApplyExecutor.shutdown();
		try {
			if(ioResultsGetAndApplyExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				LOG.debug(
					Markers.MSG, "{}: final I/O result have been got and processed properly",
					getName()
				);
			} else {
				LOG.warn(
					Markers.ERR, "{}: timeout while getting and processing the final I/O results",
					getName()
				);
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(
				LOG, Level.WARN, e,
				"{}: interrupted  while getting and processing the final I/O results", getName()
			);
		}
		
		driversMap.clear();
		ioTaskOutputs.clear();
		circularityMap.clear();

		LOG.info(
			Markers.METRICS_STDOUT,
			new MetricsStdoutLogMessage(name, lastStats, concurrencyMap, driversCountMap)
		);
		if(!preconditionJobFlag) {
			LOG.info(
				Markers.METRICS_FILE_TOTAL,
				new MetricsCsvLogMessage(lastStats, concurrencyMap, driversCountMap)
			);
			LOG.info(
				Markers.METRICS_EXT_RESULTS,
				new ExtResultsXmlLogMessage(
					name, lastStats, itemSizeMap, concurrencyMap, driversCountMap
				)
			);
		}
		
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

		if(latestIoResultsPerItem != null && ioResultsOutput != null) {
			try {
				for(final O latestItemIoResult : latestIoResultsPerItem.values()) {
					if(!ioResultsOutput.put(latestItemIoResult)) {
						LOG.debug(
							Markers.ERR,
							"{}: item info output fails to ingest, blocking the closing method",
							getName()
						);
						while(!ioResultsOutput.put(latestItemIoResult)) {
							Thread.sleep(1);
						}
						LOG.debug(Markers.MSG, "{}: closing method unblocked", getName());
					}
				}
			} catch(final InterruptedException ignored) {
			}
			latestIoResultsPerItem.clear();
		}

		if(ioResultsOutput != null) {
			ioResultsOutput.close();
			LOG.debug(Markers.MSG, "{}: closed the items output", getName());
		}

		UNCLOSED.remove(this);
		LOG.debug(Markers.MSG, "{}: closed the load monitor", getName());
	}
}
