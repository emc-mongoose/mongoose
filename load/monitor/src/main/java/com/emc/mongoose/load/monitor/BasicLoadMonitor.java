package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.DaemonBase;
import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.common.io.collection.IoBuffer;
import com.emc.mongoose.common.io.collection.LimitedQueueBuffer;
import com.emc.mongoose.load.monitor.metrics.IoTraceCsvBatchLogMessage;
import com.emc.mongoose.load.monitor.metrics.MetricsCsvLogMessage;
import com.emc.mongoose.load.monitor.metrics.MetricsStdoutLogMessage;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.io.collection.RoundRobinOutput;
import com.emc.mongoose.load.monitor.metrics.BasicIoStats;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;
import static com.emc.mongoose.ui.config.Config.LoadConfig.MetricsConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.model.io.IoType;
import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask.CompositeIoResult;
import com.emc.mongoose.model.io.task.partial.PartialIoTask.PartialIoResult;
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
	private final MetricsConfig metricsConfig;
	private final long countLimit;
	private final long sizeLimit;
	private final Thread worker;

	private final Int2ObjectMap<IoStats> ioStats = new Int2ObjectOpenHashMap<>();
	private final Int2ObjectMap<IoStats> medIoStats = new Int2ObjectOpenHashMap<>();
	private volatile Int2ObjectMap<IoStats.Snapshot> lastStats = new Int2ObjectOpenHashMap<>();
	private final LongAdder counterResults = new LongAdder();
	private volatile Output<String> itemInfoOutput;
	private final Int2IntMap totalConcurrencyMap;
	private final IoBuffer<R> ioTaskResultsQueue;

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

		this.metricsConfig = firstLoadConfig.getMetricsConfig();
		final int metricsPeriodSec = (int) metricsConfig.getPeriod();

		this.driversMap = driversMap;
		totalConcurrencyMap = new Int2IntOpenHashMap(driversMap.size());
		for(final LoadGenerator<I, O, R> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O, R>> nextDrivers = driversMap.get(nextGenerator);
			final String ioTypeName = loadConfigs.get(nextGenerator).getType().toUpperCase();
			final int ioTypeCode = IoType.valueOf(ioTypeName).ordinal();
			totalConcurrencyMap.put(
				ioTypeCode, loadConfigs.get(nextGenerator).getConcurrency() * nextDrivers.size()
			);
			ioStats.put(
				ioTypeCode, new BasicIoStats(IoType.values()[ioTypeCode].name(), metricsPeriodSec)
			);
			registerDrivers(nextDrivers);
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
		ioTaskResultsQueue = new LimitedQueueBuffer<>(
			new ArrayBlockingQueue<>(firstLoadConfig.getQueueConfig().getSize())
		);

		this.worker = new Thread(new ServiceTask(metricsPeriodSec), name);
		this.worker.setDaemon(true);

		UNCLOSED.add(this);
	}

	protected void registerDrivers(final List<StorageDriver<I, O, R>> drivers) {
		for(final StorageDriver<I, O, R> nextDriver : drivers) {
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
			int n;
			final List<R> ioTaskResultsBuff = new ArrayList<>(BATCH_SIZE);

			try {
				while(true) {
					ioTaskResultsBuff.clear();
					n = ioTaskResultsQueue.get(ioTaskResultsBuff, BATCH_SIZE);
					if(n > 0) {
						ioTaskResultsDispatch(ioTaskResultsBuff, n);
					}
					refreshStats();
					nextNanoTimeStamp = System.nanoTime();
					if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
						outputCurrentMetrics();
						prevNanoTimeStamp = nextNanoTimeStamp;
					}
					Thread.sleep(1);
				}
			} catch(final InterruptedException e) {
				LOG.debug(Markers.MSG, "Interrupted");
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to dispatch the I/O tasks results");
			}
		}

		private void ioTaskResultsDispatch(final List<R> ioTaskResults, final int n) {


			int m = n; // count of complete whole tasks

			// I/O trace logging
			if(!metricsConfig.getPrecondition()) {
				LOG.debug(
					Markers.IO_TRACE, new IoTraceCsvBatchLogMessage<>(ioTaskResults, 0, n)
				);
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
					ioTaskResult instanceof CompositeIoResult &&
						!((CompositeIoResult) ioTaskResult).getCompleteFlag()
					) {
					m --;
					continue;
				}

				itemInfo = ioTaskResult.getItemInfo();
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
						LOG.warn(
							Markers.ERR, "{}: latency {} is more than duration: {}",
							BasicLoadMonitor.this.getName(), respLatency, reqDuration
						);
					}
					if(ioTaskResult instanceof PartialIoResult) {
						ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
						if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
							ioTypeMedStats.markPartSucc(countBytesDone, reqDuration, respLatency);
						}
						m --;
					} else {
						if(itemInfoOutput != null) {
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

			if(itemInfoOutput != null) {
				final int itemsToPassCount = itemsToPass.size();
				try {
					for(
						int i = 0; i < itemsToPassCount;
						i += itemInfoOutput.put(itemsToPass, i, itemsToPassCount)
						);
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to output {} items to {}",
						itemsToPassCount, itemInfoOutput
					);
				}
			}

			counterResults.add(m);
		}

		private void refreshStats() {
			for(final int nextIoTypeCode : ioStats.keySet()) {
				lastStats.put(nextIoTypeCode, ioStats.get(nextIoTypeCode).getSnapshot());
			}
		}

		private void outputCurrentMetrics() {
			LOG.info(
				Markers.METRICS_STDOUT,
				new MetricsStdoutLogMessage(name, lastStats, totalConcurrencyMap)
			);
			if(!metricsConfig.getPrecondition()) {
				LOG.info(
					Markers.METRICS_FILE,
					new MetricsCsvLogMessage(lastStats, totalConcurrencyMap)
				);
			}
			/*for(final List<StorageDriver<I, O, R>> nextDrivers : driversMap.values()) {
				for(final StorageDriver<I, O, R> nextDriver : nextDrivers) {
					try {
						LOG.info(
							Markers.MSG, "Storage driver \"{}\" active task count: {}",
							nextDriver.toString(), nextDriver.getActiveTaskCount()
						);
					} catch(final RemoteException ignored) {
					}
				}
			}*/
		}
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
	public void put(final R ioTaskResult)
	throws IOException {
		ioTaskResultsQueue.put(ioTaskResult);
	}

	@Override
	public int put(final List<R> ioTaskResults, final int from, final int to)
	throws IOException {
		return ioTaskResultsQueue.put(ioTaskResults, from, to);
	}

	@Override
	public int put(final List<R> ioTaskResults)
	throws IOException {
		return put(ioTaskResults, 0, ioTaskResults.size());
	}

	@Override
	public Input<R> getInput()
	throws IOException {
		return null;
	}

	@Override
	public final String getName() {
		return name;
	}

	public final void setItemInfoOutput(final Output<String> itemInfoOutput) {
		this.itemInfoOutput = itemInfoOutput;
	}

	@Override
	protected void doStart()
	throws IllegalStateException {

		String authToken = null;

		for(final LoadGenerator<I, O, R> nextGenerator : driversMap.keySet()) {

			final List<StorageDriver<I, O, R>> nextGeneratorDrivers = driversMap.get(nextGenerator);

			for(final StorageDriver<I, O, R> nextDriver : nextGeneratorDrivers) {
				try {
					if(authToken == null) {
						authToken = nextDriver.getAuthToken();
					} else {
						// distribute the auth token among the storage drivers
						nextDriver.setAuthToken(authToken);
					}
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

		for(final int ioTypeCode : totalConcurrencyMap.keySet()) {
			ioStats.get(ioTypeCode).start();
		}

		worker.start();
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {

		for(final LoadGenerator<I, O, R> nextGenerator : driversMap.keySet()) {

			try {
				nextGenerator.interrupt();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to interrupt the generator {}",
					nextGenerator.toString()
				);
			}

			for(final StorageDriver<I, O, R> nextDriver : driversMap.get(nextGenerator)) {
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
	}

	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		for(final List<StorageDriver<I, O, R>> nextDrivers : driversMap.values()) {
			for(final StorageDriver<I, O, R> nextDriver : nextDrivers) {
				try {
					nextDriver.interrupt();
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.DEBUG, e, "Failed to interrupt the driver {}",
						nextDriver.toString()
					);
				}
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
			if(isDone()) {
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

		for(final LoadGenerator<I, O, R> generator : driversMap.keySet()) {

			for(final StorageDriver<I, O, R> driver : driversMap.get(generator)) {
				try {
					driver.close();
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to close the driver {}", driver);
				}
			}

			try {
				generator.close();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to close the generator {}", generator
				);
			}
		}
		
		driversMap.clear();

		LOG.info(
			Markers.METRICS_STDOUT, new MetricsStdoutLogMessage(name, lastStats, totalConcurrencyMap)
		);
		if(!metricsConfig.getPrecondition()) {
			LOG.info(
				Markers.METRICS_FILE_TOTAL, new MetricsCsvLogMessage(lastStats, totalConcurrencyMap)
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

		ioTaskResultsQueue.close();
		if(itemInfoOutput != null) {
			itemInfoOutput.close();
		}
		UNCLOSED.remove(this);
	}
}
