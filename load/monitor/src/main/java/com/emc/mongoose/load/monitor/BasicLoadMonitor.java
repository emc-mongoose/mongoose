package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.load.monitor.metrics.IoTraceCsvLogMessage;
import com.emc.mongoose.model.svc.BlockingQueueTransferTask;
import com.emc.mongoose.common.concurrent.RateThrottle;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.concurrent.WeightThrottle;
import com.emc.mongoose.model.svc.RoundRobinOutputsTransferSvcTask;
import com.emc.mongoose.load.monitor.metrics.MetricsSvcTask;
import com.emc.mongoose.model.DaemonBase;
import com.emc.mongoose.model.io.task.IoTask.Status;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.io.task.path.PathIoTask;
import static com.emc.mongoose.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;
import com.emc.mongoose.model.NamingThreadFactory;
import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.load.monitor.metrics.ExtResultsXmlLogMessage;
import com.emc.mongoose.load.monitor.metrics.IoTraceCsvBatchLogMessage;
import com.emc.mongoose.load.monitor.metrics.MetricsCsvLogMessage;
import com.emc.mongoose.load.monitor.metrics.MetricsStdoutLogMessage;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.load.monitor.metrics.BasicIoStats;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.MetricsConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.svc.TransferSvcTask;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.load.monitor.metrics.IoStats;
import com.emc.mongoose.ui.log.Loggers;

import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.Level;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 12.07.16.
 */
public class BasicLoadMonitor<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements LoadMonitor<I, O> {
	
	private final String name;
	private final Map<LoadGenerator<I, O>, List<StorageDriver<I, O>>> driversMap;
	private final boolean preconditionJobFlag;
	private final int metricsPeriodSec;
	private final int totalConcurrency;
	private final double fullLoadThreshold;
	private final long countLimit;
	private final long sizeLimit;
	private final ConcurrentMap<I, O> latestIoResultsPerItem;
	private final int batchSize;
	private final boolean isAnyCircular;
	private final Int2ObjectMap<BlockingQueue<O>> recycleQueuesMap;

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
	 @param name test step name
	 @param driversMap generator to drivers list map
	 @param loadConfigs
	 @param weightMap
	 */
	public BasicLoadMonitor(
		final String name, final Map<LoadGenerator<I, O>, List<StorageDriver<I, O>>> driversMap,
		final Int2IntMap weightMap, final Map<LoadGenerator<I, O>, LoadConfig> loadConfigs,
		final Map<LoadGenerator<I, O>, StepConfig> stepConfigs
	) {
		this.name = name;
		final StepConfig anyStepConfig = stepConfigs.values().iterator().next();
		final double rateLimit = anyStepConfig.getLimitConfig().getRate();
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

		Output<O> nextGeneratorOutput = null;
		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			try {
				nextGeneratorOutput = new RoundRobinOutputsTransferSvcTask<>(
					driversMap.get(nextGenerator), nextGenerator.getSvcTasks(),
					loadConfigs.get(nextGenerator).getBatchConfig().getSize()
				);
			} catch(final RemoteException ignored) {
			}
			ioTaskOutputs.put(nextGenerator.hashCode(), nextGeneratorOutput);
			nextGenerator.setWeightThrottle(weightThrottle);
			nextGenerator.setRateThrottle(rateThrottle);
			nextGenerator.setOutput(nextGeneratorOutput);
		}

		final MetricsConfig metricsConfig = anyStepConfig.getMetricsConfig();
		preconditionJobFlag = anyStepConfig.getPrecondition();
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
		recycleQueuesMap = new Int2ObjectOpenHashMap<>(driversMap.size());
		final LoadConfig anyLoadConfig = loadConfigs.values().iterator().next();
		this.batchSize = anyLoadConfig.getBatchConfig().getSize();
		final int queueSizeLimit = anyLoadConfig.getQueueConfig().getSize();
		int concurrencySum = 0;
		boolean anyCircularFlag = false;
		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O>> nextDrivers = driversMap.get(nextGenerator);
			final LoadConfig nextLoadConfig = loadConfigs.get(nextGenerator);
			final int nextOriginCode = nextGenerator.hashCode();
			circularityMap.put(nextOriginCode, nextLoadConfig.getCircular());
			if(circularityMap.get(nextOriginCode)) {
				anyCircularFlag = true;
				recycleQueuesMap.put(nextOriginCode, new ArrayBlockingQueue<O>(queueSizeLimit));
			}
			final String ioTypeName = nextLoadConfig.getType().toUpperCase();
			final int ioTypeCode = IoType.valueOf(ioTypeName).ordinal();
			driversCountMap.put(ioTypeCode, nextDrivers.size());
			try {
				final int ioTypeSpecificConcurrency = nextDrivers.get(0).getConcurrencyLevel();
				concurrencySum += ioTypeSpecificConcurrency;
				concurrencyMap.put(ioTypeCode, ioTypeSpecificConcurrency);
			} catch(final RemoteException e) {
				LogUtil.exception(Level.ERROR, e, "Failed to invoke the remote method");
			}
			ioStats.put(ioTypeCode, new BasicIoStats(metricsPeriodSec));
			if(medIoStats != null) {
				medIoStats.put(ioTypeCode, new BasicIoStats(metricsPeriodSec));
			}
			itemSizeMap.put(nextGenerator.getIoType().ordinal(), nextGenerator.getItemSizeEstimate());
		}
		this.totalConcurrency = concurrencySum;
		this.isAnyCircular = anyCircularFlag;
		if(isAnyCircular) {
			latestIoResultsPerItem = new ConcurrentHashMap<>(queueSizeLimit);
		} else {
			latestIoResultsPerItem = null;
		}

		long countLimitSum = 0;
		long sizeLimitSum = 0;
		for(final LoadGenerator<I, O> nextLoadGenerator : loadConfigs.keySet()) {
			final LimitConfig nextLimitConfig = stepConfigs.get(nextLoadGenerator).getLimitConfig();
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
	}

	private boolean isDoneCountLimit() {
		if(countLimit > 0) {
			if(counterResults.sum() >= countLimit) {
				Loggers.MSG.debug(
					"{}: count limit reached, {} results >= {} limit", name, counterResults.sum(),
					countLimit
				);
				return true;
			}
			long succCountSum = 0;
			long failCountSum = 0;
			for(final int ioTypeCode : lastStats.keySet()) {
				succCountSum += lastStats.get(ioTypeCode).getSuccCount();
				failCountSum += lastStats.get(ioTypeCode).getFailCount();
				if(succCountSum + failCountSum >= countLimit) {
					Loggers.MSG.debug(
						"{}: count limit reached, {} successful + {} failed >= {} limit",
						name, succCountSum, failCountSum, countLimit
					);
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
					Loggers.MSG.debug(
						"{}: size limit reached, done {} >= {} limit",
						SizeInBytes.formatFixedSize(sizeSum), sizeLimit
					);
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
					Level.WARN, e, "Failed to communicate with load generator \"{}\"",
					nextLoadGenerator
				);
			}
		}
		return counterResults.longValue() >= generatedIoTasks;
	}

	// issue SLTM-938 fix
	private boolean nothingToRecycle() {
		if(driversMap.size() == 1) {
			final LoadGenerator<I, O> soleLoadGenerator = driversMap.keySet().iterator().next();
			try {
				if(soleLoadGenerator.isStarted()) {
					return false;
				}
			} catch(final RemoteException e) {
				LogUtil.exception(Level.WARN, e, "Failed to check the load generator state");
			}
			// load generator has done its work
			final long generatedIoTasks = soleLoadGenerator.getGeneratedIoTasksCount();
			if(
				circularityMap.get(soleLoadGenerator.hashCode()) && // circular load job
				counterResults.sum() >= generatedIoTasks && // all generated I/O tasks executed at least once
				latestIoResultsPerItem.size() == 0 // no successful I/O results
			) {
				return true;
			}
		}
		return false;
	}

	private boolean isDone() {
		if(isDoneCountLimit()) {
			Loggers.MSG.debug("{}: done due to max count done state", getName());
			return true;
		}
		if(isDoneSizeLimit()) {
			Loggers.MSG.debug("{}: done due to max size done state", getName());
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
					Level.WARN, e, "Failed to communicate with load generator \"{}\"",
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
							Level.WARN, e, "Failed to communicate with storage driver \"{}\"",
							nextStorageDriver
						);
					}
				} catch(final RemoteException e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to communicate with storage driver \"{}\"",
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
	public final boolean put(final O ioTaskResult) {
		
		// I/O trace logging
		if(!preconditionJobFlag && Loggers.IO_TRACE.isDebugEnabled()) {
			Loggers.IO_TRACE.debug(new IoTraceCsvLogMessage<>(ioTaskResult));
		}
		
		if( // account only completed composite I/O tasks
			ioTaskResult instanceof CompositeIoTask &&
				!((CompositeIoTask) ioTaskResult).allSubTasksDone()
			) {
			return true;
		}
		
		final int ioTypeCode = ioTaskResult.getIoType().ordinal();
		final IoStats ioTypeStats = ioStats.get(ioTypeCode);
		final IoStats ioTypeMedStats = medIoStats == null ? null : medIoStats.get(ioTypeCode);
		final IoTask.Status status = ioTaskResult.getStatus();
		
		if(Status.SUCC.equals(status)) {
			final long reqDuration = ioTaskResult.getDuration();
			final long respLatency = ioTaskResult.getLatency();
			final long countBytesDone;
			if(ioTaskResult instanceof DataIoTask) {
				countBytesDone = ((DataIoTask) ioTaskResult).getCountBytesDone();
			} else if(ioTaskResult instanceof PathIoTask) {
				countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
			} else {
				countBytesDone = 0;
			}
			
			if(ioTaskResult instanceof PartialIoTask) {
				ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
				if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
					ioTypeMedStats.markPartSucc(countBytesDone, reqDuration, respLatency);
				}
			} else {
				final int originCode = ioTaskResult.getOriginCode();
				if(circularityMap.get(originCode)) {
					final I item = ioTaskResult.getItem();
					latestIoResultsPerItem.put(item, ioTaskResult);
					if(rateThrottle != null) {
						if(!rateThrottle.tryAcquire(ioTaskResult)) {
							return false;
						}
					}
					if(weightThrottle != null) {
						if(!weightThrottle.tryAcquire(originCode)) {
							return false;
						}
					}
					if(!recycleQueuesMap.get(originCode).add(ioTaskResult)) {
						return false;
					}
				} else if(ioResultsOutput != null){
					try {
						if(!ioResultsOutput.put(ioTaskResult)) {
							return false;
						}
					} catch(final EOFException e) {
						LogUtil.exception(
							Level.DEBUG, e, "I/O task destination end of input"
						);
					} catch(final NoSuchObjectException e) {
						LogUtil.exception(
							Level.DEBUG, e,
							"Remote I/O task destination is not more available"
						);
					} catch(final IOException e) {
						LogUtil.exception(
							Level.WARN, e, "Failed to put the I/O task to the destionation"
						);
					}
				}
				
				ioTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
				if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
					ioTypeMedStats.markSucc(countBytesDone, reqDuration, respLatency);
				}
				counterResults.increment();
			}
		} else if(!Status.CANCELLED.equals(status)) {
			Loggers.ERR.debug("{}: {}", ioTaskResult.toString(), status.toString());
			ioTypeStats.markFail();
			if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
				ioTypeMedStats.markFail();
			}
		}
		return true;
	}
	
	@Override
	public final int put(final List<O> ioTaskResults, final int from, final int to) {
		
		// I/O trace logging
		if(!preconditionJobFlag && Loggers.IO_TRACE.isDebugEnabled()) {
			Loggers.IO_TRACE.debug(new IoTraceCsvBatchLogMessage<>(ioTaskResults, from, to));
		}

		int originCode;
		I item;
		O ioTaskResult;
		int ioTypeCode;
		Status status;
		long reqDuration;
		long respLatency;
		long countBytesDone = 0;
		IoStats ioTypeStats, ioTypeMedStats;

		int i;
		for(i = from; i < to; i ++) {

			ioTaskResult = ioTaskResults.get(i);
			
			if( // account only completed composite I/O tasks
				ioTaskResult instanceof CompositeIoTask &&
				!((CompositeIoTask) ioTaskResult).allSubTasksDone()
			) {
				continue;
			}

			originCode = ioTaskResult.getOriginCode();
			ioTypeCode = ioTaskResult.getIoType().ordinal();
			status = ioTaskResult.getStatus();
			reqDuration = ioTaskResult.getDuration();
			respLatency = ioTaskResult.getLatency();
			if(ioTaskResult instanceof DataIoTask) {
				countBytesDone = ((DataIoTask) ioTaskResult).getCountBytesDone();
			} else if(ioTaskResult instanceof PathIoTask) {
				countBytesDone = ((PathIoTask) ioTaskResult).getCountBytesDone();
			}

			ioTypeStats = ioStats.get(ioTypeCode);
			ioTypeMedStats = medIoStats == null ? null : medIoStats.get(ioTypeCode);

			if(Status.SUCC.equals(status)) {
				if(ioTaskResult instanceof PartialIoTask) {
					ioTypeStats.markPartSucc(countBytesDone, reqDuration, respLatency);
					if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
						ioTypeMedStats.markPartSucc(countBytesDone, reqDuration, respLatency);
					}
				} else {
					if(circularityMap.get(originCode)) {
						item = ioTaskResult.getItem();
						latestIoResultsPerItem.put(item, ioTaskResult);
						if(rateThrottle != null) {
							if(!rateThrottle.tryAcquire(ioTaskResult)) {
								break;
							}
						}
						if(weightThrottle != null && !weightThrottle.tryAcquire(originCode)) {
							break;
						}
						if(!recycleQueuesMap.get(originCode).add(ioTaskResult)) {
							break;
						}
					} else if(ioResultsOutput != null){
						try {
							if(!ioResultsOutput.put(ioTaskResult)) {
								break;
							}
						} catch(final EOFException e) {
							LogUtil.exception(
								Level.DEBUG, e, "I/O task destination end of input"
							);
						} catch(final NoSuchObjectException e) {
							LogUtil.exception(
								Level.DEBUG, e,
								"Remote I/O task destination is not more available"
							);
						} catch(final IOException e) {
							LogUtil.exception(
								Level.WARN, e, "Failed to put the I/O task to the destionation"
							);
						}
					}
					
					ioTypeStats.markSucc(countBytesDone, reqDuration, respLatency);
					if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
						ioTypeMedStats.markSucc(countBytesDone, reqDuration, respLatency);
					}
					counterResults.increment();
				}
			} else if(!Status.CANCELLED.equals(status)) {
				Loggers.ERR.debug("{}: {}", ioTaskResult.toString(), status.toString());
				ioTypeStats.markFail();
				if(ioTypeMedStats != null && ioTypeMedStats.isStarted()) {
					ioTypeMedStats.markFail();
				}
			}
		}
		
		return i - from;
	}
	
	@Override
	public final int put(final List<O> ioTaskResults) {
		return put(ioTaskResults, 0, ioTaskResults.size());
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
					LogUtil.exception(Level.WARN, e, "Failed to invoke the remote method");
				}
			}
		}
		return totalActiveTaskCount;
	}
	
	@Override
	protected void doStart()
	throws IllegalStateException {
		super.doStart();

		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			final List<StorageDriver<I, O>> nextGeneratorDrivers = driversMap.get(nextGenerator);
			for(final StorageDriver<I, O> nextDriver : nextGeneratorDrivers) {
				try {
					nextDriver.start();
				} catch(final IllegalStateException | RemoteException e) {
					LogUtil.exception(
						Level.WARN, e, "Failed to start the driver {}", nextDriver.toString()
					);
				}
			}
			try {
				nextGenerator.start();
			} catch(final IllegalStateException | RemoteException e) {
				LogUtil.exception(
					Level.WARN, e, "Failed to start the generator {}", nextGenerator.toString()
				);
			}
		}

		for(final int ioTypeCode : concurrencyMap.keySet()) {
			ioStats.get(ioTypeCode).start();
		}

		try {
			svcTasks.add(
				new MetricsSvcTask(
					this, name, metricsPeriodSec, preconditionJobFlag, driversCountMap,
					concurrencyMap, ioStats, lastStats, medIoStats, lastMedStats, itemSizeMap,
					(int) (fullLoadThreshold * totalConcurrency)
				)
			);
		} catch(final RemoteException ignore) {
		}
		for(final int originCode : recycleQueuesMap.keySet()) {
			if(circularityMap.get(originCode)) {
				svcTasks.add(
					new BlockingQueueTransferTask<>(
						recycleQueuesMap.get(originCode), ioTaskOutputs.get(originCode), batchSize,
						svcTasks
					)
				);
			}
		}

		final List<StorageDriver<I, O>> drivers = new ArrayList<>();
		for(final List<StorageDriver<I, O>> nextGeneratorDrivers : driversMap.values()) {
			for(final StorageDriver<I, O> nextDriver : nextGeneratorDrivers) {
				svcTasks.add(
					new TransferSvcTask<>(svcTasks, name, nextDriver, this, batchSize)
				);
			}
			//drivers.addAll(nextGeneratorDrivers);
		}
		//svcTasks.add(
		//	new RoundRobinInputsTransferSvcTask<>(name, this, drivers, batchSize, svcTasks)
		//);
	}

	@Override
	protected void doShutdown()
	throws IllegalStateException {
		
		final ExecutorService shutdownExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(),
			new NamingThreadFactory("shutdownWorker", true)
		);

		for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
			shutdownExecutor.submit(
				() -> {
					try(
						final Instance ctx = CloseableThreadContext
							.put(KEY_STEP_NAME, name)
							.put(KEY_CLASS_NAME, getClass().getSimpleName())
					) {
						nextGenerator.interrupt();
						Loggers.MSG.debug(
							"{}: load generator \"{}\" interrupted", getName(),
							nextGenerator.toString()
						);
					} catch(final RemoteException e) {
						LogUtil.exception(
							Level.WARN, e, "{}: failed to interrupt the generator {}", getName(),
							nextGenerator.toString()
						);
					}
				}
			);
			for(final StorageDriver<I, O> driver : driversMap.get(nextGenerator)) {
				shutdownExecutor.submit(
					() -> {
						try(
							final Instance ctx = CloseableThreadContext
								.put(KEY_STEP_NAME, name)
								.put(KEY_CLASS_NAME, getClass().getSimpleName())
						) {
							driver.shutdown();
							Loggers.MSG.info(
								"{}: next storage driver {} shutdown", getName(),
								(
									(driver instanceof Service)?
										((Service) driver).getName() + " @ " +
											ServiceUtil.getAddress((Service) driver) :
										driver.toString()
								)
							);
						} catch(final RemoteException e) {
							LogUtil.exception(
								Level.WARN, e, "failed to shutdown the driver {}", getName(),
								driver.toString()
							);
						}
					}
				);
			}
		}
		
		Loggers.MSG.info("{}: shutting down the storage drivers...", getName());
		shutdownExecutor.shutdown();
		try {
			if(shutdownExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				Loggers.MSG.debug("{}: load monitor was shut down properly", getName());
			} else {
				Loggers.ERR.warn("{}: load monitor shutdown timeout", getName());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "{}: load monitor shutdown interrupted", getName());
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
		Loggers.MSG.debug(
			"{}: await for the done condition at most for {}[s]", getName(),
			TimeUnit.MILLISECONDS.toSeconds(timeOutMilliSec)
		);
		t = System.currentTimeMillis();
		while(System.currentTimeMillis() - t < timeOutMilliSec) {
			synchronized(state) {
				state.wait(100);
			}
			if(isInterrupted()) {
				Loggers.MSG.debug("{}: await exit due to \"interrupted\" state", getName());
				return true;
			}
			if(isClosed()) {
				Loggers.MSG.debug("{}: await exit due to \"closed\" state", getName());
				return true;
			}
			if(isDone()) {
				Loggers.MSG.debug("{}: await exit due to \"done\" state", getName());
				return true;
			}
			if(!isAnyCircular && allIoTasksCompleted()) {
				Loggers.MSG.debug(
					"{}: await exit because all I/O tasks have been completed", getName()
				);
				return true;
			}
			// issue SLTM-938 fix
			if(nothingToRecycle()) {
				Loggers.ERR.debug(
					"{}: exit because there's no I/O task to recycle (all failed)", getName()
				);
				return true;
			}
		}
		Loggers.MSG.debug("{}: await exit due to timeout", getName());
		return false;
	}

	@Override
	protected final void doInterrupt()
	throws IllegalStateException {
		
		final ExecutorService interruptExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(),
			new NamingThreadFactory("interruptWorker", true)
		);

		synchronized(driversMap) {
			for(final LoadGenerator<I, O> nextGenerator : driversMap.keySet()) {
				for(final StorageDriver<I, O> driver : driversMap.get(nextGenerator)) {
					interruptExecutor.submit(
						() -> {
							try(
								final Instance ctx = CloseableThreadContext
									.put(KEY_STEP_NAME, name)
									.put(KEY_CLASS_NAME, getClass().getSimpleName())
							) {
								driver.interrupt();
								Loggers.MSG.info(
									"{}: next storage driver {} interrupted", getName(),
									(
										(driver instanceof Service)?
											((Service) driver).getName() + " @ " +
												ServiceUtil.getAddress((Service) driver) :
											driver.toString()
									)
								);
							} catch(final RemoteException e) {
								LogUtil.exception(
									Level.DEBUG, e, "{}: failed to interrupt the driver {}",
									getName(), driver.toString()
								);
							}
						}
					);
				}
			}
		}
		
		Loggers.MSG.info("{}: interrupting the storage drivers...", getName());
		interruptExecutor.shutdown();
		try {
			if(interruptExecutor.awaitTermination(100, TimeUnit.SECONDS)) {
				Loggers.MSG.debug("{}: storage drivers have been interrupted properly", getName());
			} else {
				Loggers.ERR.warn("{}: storage drivers interrupting timeout", getName());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(
				Level.WARN, e, "{}: storage drivers interrupting interrupted", getName()
			);
		}
		
		synchronized(svcTasks) {
			// stop all service tasks
			for(final SvcTask svcTask : svcTasks) {
				try {
					svcTask.close();
				} catch(final IOException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to stop the service task {}", svcTask
					);
				}
			}
			svcTasks.clear();
		}

		Loggers.MSG.debug("{}: interrupted the load monitor", getName());
	}

	@Override
	protected final void doClose()
	throws IOException {
		
		super.doClose();
		
		final ExecutorService ioResultsExecutor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount(), new NamingThreadFactory("ioResultsWorker", true)
		);
		
		synchronized(driversMap) {

			for(final LoadGenerator<I, O> generator : driversMap.keySet()) {
				for(final StorageDriver<I, O> driver : driversMap.get(generator)) {
					ioResultsExecutor.submit(
						() -> {
							try(
								final Instance ctx = CloseableThreadContext
									.put(KEY_STEP_NAME, name)
									.put(KEY_CLASS_NAME, getClass().getSimpleName())
							) {
								try {
									final List<O> finalResults = driver.getAll();
									if(finalResults != null) {
										final int finalResultsCount = finalResults.size();
										if(finalResultsCount > 0) {
											Loggers.MSG.debug(
												"{}: the driver \"{}\" returned {} final I/O " +
													"results to process",
												getName(), driver.toString(), finalResults.size()
											);
											put(finalResults, 0, finalResultsCount);
										}
									}
								} catch(final Throwable cause) {
									LogUtil.exception(
										Level.WARN, cause,
										"{}: failed to process the final results for the driver {}",
										getName(), driver.toString()
									);
								}
								try {
									driver.close();
									Loggers.MSG.info("{}: next storage driver {} closed", getName(),
										((driver instanceof Service) ?
											((Service) driver).getName() + " @ " +
												ServiceUtil.getAddress((Service) driver) :
											driver.toString())
									);
								} catch(final NoSuchObjectException ignored) {
									// closing causes this normally
								} catch(final IOException e) {
									LogUtil.exception(
										Level.WARN, e, "{}: failed to close the driver {}",
										getName(), driver.toString()
									);
								}
							}
						}
					);
				}
				
				try {
					generator.close();
					Loggers.MSG.debug(
						"{}: the load generator \"{}\" has been closed", getName(), generator
					);
				} catch(final IOException e) {
					LogUtil.exception(
						Level.WARN, e, "{}: failed to close the generator {}", getName(), generator
					);
				}
			}

			ioResultsExecutor.shutdown();

			try {
				if(ioResultsExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
					Loggers.MSG.debug(
						"{}: final I/O result have been got and processed properly", getName()
					);
				} else {
					Loggers.ERR.warn(
						"{}: timeout while getting and processing the final I/O results", getName()
					);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(
					Level.WARN, e,
					"{}: interrupted  while getting and processing the final I/O results", getName()
				);
			}
		
			driversMap.clear();
		}
		
		ioTaskOutputs.clear();
		circularityMap.clear();
		for(final BlockingQueue<O> recycleQueue : recycleQueuesMap.values()) {
			recycleQueue.clear();
		}
		recycleQueuesMap.clear();
		
		Loggers.METRICS_STD_OUT.info(
			new MetricsStdoutLogMessage(name, lastStats, concurrencyMap, driversCountMap)
		);
		if(!preconditionJobFlag) {
			Loggers.METRICS_FILE_TOTAL.info(
				new MetricsCsvLogMessage(lastStats, concurrencyMap, driversCountMap)
			);
			Loggers.METRICS_EXT_RESULTS_FILE.info(
				new ExtResultsXmlLogMessage(
					name, lastStats, itemSizeMap, concurrencyMap, driversCountMap
				)
			);
		}
		
		for(final IoStats nextStats : ioStats.values()) {
			nextStats.close();
		}
		ioStats.clear();
		
		if(medIoStats != null && !medIoStats.isEmpty()) {
			Loggers.MSG.info(
				"{}: The active tasks count is below the threshold of {}, " +
					"stopping the additional metrics accounting",
				name, (int) (fullLoadThreshold * totalConcurrency)
			);
			Loggers.METRICS_THRESHOLD_FILE_TOTAL.info(
				new MetricsCsvLogMessage(lastMedStats, concurrencyMap, driversCountMap)
			);
			Loggers.METRICS_THRESHOLD_EXT_RESULTS_FILE.info(
				new ExtResultsXmlLogMessage(
					name, lastMedStats, itemSizeMap, concurrencyMap, driversCountMap
				)
			);
			for(final IoStats nextMedStats : medIoStats.values()) {
				if(nextMedStats.isStarted()) {
					nextMedStats.close();
				}
			}
			medIoStats.clear();
		}

		if(latestIoResultsPerItem != null && ioResultsOutput != null) {
			try {
				for(final O latestItemIoResult : latestIoResultsPerItem.values()) {
					if(!ioResultsOutput.put(latestItemIoResult)) {
						Loggers.ERR.debug(
							"{}: item info output fails to ingest, blocking the closing method",
							getName()
						);
						while(!ioResultsOutput.put(latestItemIoResult)) {
							Thread.sleep(1);
						}
						Loggers.MSG.debug("{}: closing method unblocked", getName());
					}
				}
			} catch(final InterruptedException ignored) {
			}
			latestIoResultsPerItem.clear();
		}

		if(ioResultsOutput != null) {
			ioResultsOutput.close();
			Loggers.MSG.debug("{}: closed the items output", getName());
		}

		Loggers.MSG.debug("{}: closed the load monitor", getName());
	}
}
