package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.model.DaemonBase;
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
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
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.load.monitor.metrics.IoStats;
import com.emc.mongoose.ui.log.Markers;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 12.07.16.
 */
public class BasicLoadMonitor<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends DaemonBase
implements LoadMonitor<R> {

	private static final Logger LOG = LogManager.getLogger();

	private final String name;
	private final Map<LoadGenerator<I, O, R>, List<StorageDriver<I, O, R>>> driversMap;
	private final boolean preconditionJobFlag;
	private final int metricsPeriodSec;
	private final long countLimit;
	private final long sizeLimit;
	private final ConcurrentMap<String, String> uniqueItems;
	private final boolean isAnyCircular;
	private final ThreadPoolExecutor svcTaskExecutor;

	private final Int2ObjectMap<IoStats> ioStats = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectMap<IoStats> medIoStats = new Int2ObjectOpenHashMap<>();
	private volatile Int2ObjectMap<IoStats.Snapshot> lastStats = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectMap<SizeInBytes> itemSizeMap = new Int2ObjectOpenHashMap<>();
	private final LongAdder counterResults = new LongAdder();
	private volatile Output<String> itemInfoOutput;
	private final Int2IntMap concurrencyMap;
	private final Int2IntMap driversCountMap;
	private final Object2BooleanMap<LoadGenerator<I, O, R>> circularityMap;

	/**
	 Single load job constructor
	 @param name
	 @param loadGenerator
	 @param driversMap
	 @param loadConfig
	 */
	public BasicLoadMonitor(
		final String name, final LoadGenerator<I, O, R> loadGenerator,
		final List<StorageDriver<I, O, R>> driversMap, final LoadConfig loadConfig
	) {
		this(
			name,
			new HashMap<LoadGenerator<I, O, R>, List<StorageDriver<I, O, R>>>() {{
				put(loadGenerator, driversMap);
			}},
			new HashMap<LoadGenerator<I, O, R>, LoadConfig>() {{
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
		final Map<LoadGenerator<I, O, R>, List<StorageDriver<I, O, R>>> driversMap,
		final Map<LoadGenerator<I, O, R>, LoadConfig> loadConfigs
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
		final Map<LoadGenerator<I, O, R>, List<StorageDriver<I, O, R>>> driversMap,
		final Map<LoadGenerator<I, O, R>, LoadConfig> loadConfigs,
		final Object2IntMap<LoadGenerator<I, O, R>> weightMap
	) {
		this.name = name;

		final LoadConfig firstLoadConfig = loadConfigs.get(loadConfigs.keySet().iterator().next());
		final double rateLimit = firstLoadConfig.getLimitConfig().getRate();
		final Throttle<Object> rateThrottle;
		if(rateLimit > 0) {
			rateThrottle = new RateThrottle<>(rateLimit);
		} else {
			rateThrottle = null;
		}

		final Throttle<LoadGenerator<I, O, R>> weightThrottle;
		if(weightMap == null || weightMap.size() == 0 || weightMap.size() == 1) {
			weightThrottle = null;
		} else {
			weightThrottle = new WeightThrottle<>(weightMap);
		}

		Output<O> nextGeneratorOutput;
		for(final LoadGenerator<I, O, R> nextGenerator : driversMap.keySet()) {
			nextGeneratorOutput = new RoundRobinOutput<>(driversMap.get(nextGenerator));
			nextGenerator.setWeightThrottle(weightThrottle);
			nextGenerator.setRateThrottle(rateThrottle);
			nextGenerator.setOutput(nextGeneratorOutput);
		}

		final MetricsConfig metricsConfig = firstLoadConfig.getMetricsConfig();
		preconditionJobFlag = metricsConfig.getPrecondition();
		metricsPeriodSec = (int) metricsConfig.getPeriod();

		this.driversMap = driversMap;
		concurrencyMap = new Int2IntOpenHashMap(driversMap.size());
		driversCountMap = new Int2IntOpenHashMap(driversMap.size());
		circularityMap = new Object2BooleanArrayMap<>(driversMap.size());
		int driversCount = 0;
		boolean anyCircularFlag = false;
		for(final LoadGenerator<I, O, R> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O, R>> nextDrivers = driversMap.get(nextGenerator);
			driversCount += nextDrivers.size();
			final LoadConfig nextLoadConfig = loadConfigs.get(nextGenerator);
			circularityMap.put(nextGenerator, nextLoadConfig.getCircular());
			if(circularityMap.getBoolean(nextGenerator)) {
				anyCircularFlag = true;
			}
			final String ioTypeName = nextLoadConfig.getType().toUpperCase();
			final int ioTypeCode = IoType.valueOf(ioTypeName).ordinal();
			driversCountMap.put(ioTypeCode, nextDrivers.size());
			concurrencyMap.put(ioTypeCode, loadConfigs.get(nextGenerator).getConcurrency());
			ioStats.put(
				ioTypeCode, new BasicIoStats(IoType.values()[ioTypeCode].name(), metricsPeriodSec)
			);
			itemSizeMap.put(nextGenerator.getIoType().ordinal(), nextGenerator.getAvgItemSize());
		}
		this.isAnyCircular = anyCircularFlag;
		if(isAnyCircular) {
			uniqueItems = new ConcurrentHashMap<>(firstLoadConfig.getQueueConfig().getSize());
		} else {
			uniqueItems = null;
		}

		long countLimitSum = 0;
		long sizeLimitSum = 0;
		for(final LoadGenerator<I, O, R> nextLoadGenerator : loadConfigs.keySet()) {
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

		this.svcTaskExecutor = new ThreadPoolExecutor(
			driversCount + 1, driversCount + 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
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
		for(final LoadGenerator<I, O, R> nextLoadGenerator : driversMap.keySet()) {
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

		for(final LoadGenerator<I, O, R> nextLoadGenerator : driversMap.keySet()) {

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

			for(final StorageDriver<I, O, R> nextStorageDriver : driversMap.get(nextLoadGenerator)) {
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
	public final void setItemInfoOutput(final Output<String> itemInfoOutput) {
		this.itemInfoOutput = itemInfoOutput;
	}

	@Override
	public final void processIoResults(
		final List<R> ioTaskResults, final int n, final boolean isCircular
	) {

		int m = n; // count of complete whole tasks
		int itemInfoCommaPos;

		// I/O trace logging
		if(LOG.isDebugEnabled(Markers.IO_TRACE)) {
			LOG.debug(Markers.IO_TRACE, new IoTraceCsvBatchLogMessage<>(ioTaskResults, 0, n));
		}

		R ioTaskResult;
		DataIoResult dataIoTaskResult;
		int ioTypeCode;
		int statusCode;
		String itemInfo;
		long reqDuration;
		long respLatency;
		long countBytesDone = 0;
		ioTaskResult = ioTaskResults.get(0);
		final boolean isDataTransferred = ioTaskResult instanceof DataIoResult;
		IoStats ioTypeStats, ioTypeMedStats;

		final List<String> itemsToPass = itemInfoOutput == null ? null : new ArrayList<>(n);

		for(int i = 0; i < n; i ++) {

			if(i > 0) {
				ioTaskResult = ioTaskResults.get(i);
			}

			if( // account only completed composite I/O tasks
				ioTaskResult instanceof CompositeIoTask.CompositeIoResult &&
					!((CompositeIoTask.CompositeIoResult) ioTaskResult).getCompleteFlag()
				) {
				m --;
				continue;
			}

			ioTypeCode = ioTaskResult.getIoTypeCode();
			statusCode = ioTaskResult.getStatusCode();
			reqDuration = ioTaskResult.getDuration();
			respLatency = ioTaskResult.getLatency();
			if(isDataTransferred) {
				dataIoTaskResult = (DataIoResult) ioTaskResult;
				countBytesDone = dataIoTaskResult.getCountBytesDone();
			}

			ioTypeStats = ioStats.get(ioTypeCode);
			ioTypeMedStats = medIoStats.get(ioTypeCode);

			if(statusCode == IoTask.Status.SUCC.ordinal()) {
				if(respLatency > 0 && respLatency > reqDuration) {
					LOG.debug(Markers.ERR, "Dropping invalid latency value {}", respLatency);
				}
				if(ioTaskResult instanceof PartialIoTask.PartialIoResult) {
					ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
					if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
						ioTypeMedStats.markPartSucc(countBytesDone, reqDuration, respLatency);
					}
					m --;
				} else {
					itemInfo = ioTaskResult.getItemInfo();
					if(isCircular) {
						itemInfoCommaPos = itemInfo.indexOf(',', 0);
						if(itemInfoCommaPos > 0) {
							uniqueItems.put(itemInfo.substring(0, itemInfoCommaPos), itemInfo);
						} else {
							uniqueItems.put(itemInfo, itemInfo);
						}
					} else if(itemInfoOutput != null) {
						itemsToPass.add(itemInfo);
					}
					// update the metrics with success
					ioTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
					if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
						ioTypeMedStats.markSucc(countBytesDone, reqDuration, respLatency);
					}
				}

			} else {
				ioTypeStats.markFail();
				if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
					ioTypeMedStats.markFail();
				}
			}
		}

		if(!isCircular && itemInfoOutput != null) {
			final int itemsToPassCount = itemsToPass.size();
			try {
				for(
					int i = 0; i < itemsToPassCount;
					i += itemInfoOutput.put(itemsToPass, i, itemsToPassCount)
				) {
					LockSupport.parkNanos(1);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to output {} items to {}", itemsToPassCount,
					itemInfoOutput
				);
			}
		}

		counterResults.add(m);
	}

	@Override
	protected void doStart()
	throws IllegalStateException {

		for(final LoadGenerator<I, O, R> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O, R>> nextGeneratorDrivers = driversMap.get(nextGenerator);
			for(final StorageDriver<I, O, R> nextDriver : nextGeneratorDrivers) {
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
		for(final LoadGenerator<I, O, R> generator : driversMap.keySet()) {
			for(final StorageDriver<I, O, R> driver : driversMap.get(generator)) {
				svcTaskExecutor.submit(
					new GetAndProcessIoResultsSvcTask<>(this, driver, circularityMap.get(generator))
				);
			}
		}
		svcTaskExecutor.shutdown();
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {

		for(final LoadGenerator<I, O, R> nextGenerator : driversMap.keySet()) {

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

			for(final StorageDriver<I, O, R> nextDriver : driversMap.get(nextGenerator)) {
				try {
					nextDriver.shutdown();
					LOG.debug(
						Markers.MSG, "{}: storage driver \"{}\" shut down", getName(),
						nextDriver.toString()
					);
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "failed to shutdown the driver {}",
						getName(), nextDriver.toString()
					);
				}
			}
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
			if(isDone()) {
				LOG.debug(Markers.MSG, "{}: await exit due to \"done\" state", getName());
				return true;
			}
			if(!isAnyCircular && allIoTasksCompleted()) {
				LOG.debug(
					Markers.MSG, "{}: await exit due to IO Tasks have been completed", getName()
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

		for(final LoadGenerator<I, O, R> nextGenerator : driversMap.keySet()) {
			for(final StorageDriver<I, O, R> nextDriver : driversMap.get(nextGenerator)) {
				try {
					nextDriver.interrupt();
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.DEBUG, e, "{}: failed to interrupt the driver {}",
						getName(), nextDriver.toString()
					);
				}
			}
		}

		svcTaskExecutor.shutdownNow();
		try {
			if(!svcTaskExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				LOG.error(
					Markers.ERR, "{}: failed to terminate the service tasks in 1 second", getName()
				);
			}
		} catch(final InterruptedException e) {
			assert false;
		}
		LOG.debug(Markers.MSG, "{}: interrupted the load monitor", getName());
	}

	@Override
	protected final void doClose()
	throws IOException {

		for(final LoadGenerator<I, O, R> generator : driversMap.keySet()) {

			for(final StorageDriver<I, O, R> driver : driversMap.get(generator)) {

				try {
					final List<R> finalResults = driver.getResults();
					if(finalResults != null) {
						final int finalResultsCount = finalResults.size();
						if(finalResultsCount > 0) {
							LOG.debug(
								Markers.MSG,
								"{}: the driver \"{}\" returned {} final I/O results to process",
								getName(), driver, finalResults
							);
							processIoResults(
								finalResults, finalResultsCount, circularityMap.get(generator)
							);
						}
					}
				} catch(final Throwable cause) {
					LogUtil.exception(
						LOG, Level.WARN, cause,
						"{}: failed to process the final results for the driver {}", getName(),
						driver
					);
				}

				try {
					driver.close();
					LOG.debug(
						Markers.MSG, "{}: the storage driver \"{}\" has been closed", getName(),
						driver
					);
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "{}: failed to close the driver {}", getName(), driver
					);
				}
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
		
		driversMap.clear();

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

		if(uniqueItems != null && itemInfoOutput != null) {
			try {
				for(final String itemInfo : uniqueItems.values()) {
					if(!itemInfoOutput.put(itemInfo)) {
						LOG.debug(
							Markers.ERR,
							"{}: item info output fails to ingest, blocking the closing method",
							getName()
						);
						while(!itemInfoOutput.put(itemInfo)) {
							Thread.sleep(1);
						}
						LOG.debug(Markers.MSG, "{}: closing method unblocked", getName());
					}
				}
			} catch(final InterruptedException ignored) {
			}
			uniqueItems.clear();
		}

		if(itemInfoOutput != null) {
			itemInfoOutput.close();
			LOG.debug(Markers.MSG, "{}: closed the items output", getName());
		}

		UNCLOSED.remove(this);
		LOG.debug(Markers.MSG, "{}: closed the load monitor", getName());
	}
}
