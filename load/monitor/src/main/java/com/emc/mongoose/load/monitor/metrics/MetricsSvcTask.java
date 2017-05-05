package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.concurrent.SvcTaskBase;
import com.emc.mongoose.model.load.LoadMonitor;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import com.emc.mongoose.ui.log.Loggers;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static java.lang.System.nanoTime;

/**
 Created by andrey on 31.03.17.
 */
public class MetricsSvcTask
extends SvcTaskBase {
	
	private final Lock exclusiveInvocationLock = new ReentrantLock();
	private final long metricsPeriodNanoSec;
	private final Int2ObjectMap<IoStats> ioStats;
	private final Int2ObjectMap<IoStats.Snapshot> lastStats;
	private final Int2ObjectMap<IoStats> medIoStats;
	private final Int2ObjectMap<IoStats.Snapshot> lastMedStats;
	private final Int2ObjectMap<SizeInBytes> itemSizeMap;
	private final String stepName;
	private final Int2IntMap driversCountMap;
	private final Int2IntMap concurrencyMap;
	private final boolean volatileOutputFlag;

	private volatile long prevNanoTimeStamp;
	private volatile long nextNanoTimeStamp;

	private final LoadMonitor loadMonitor;
	private final int activeTasksThreshold;

	private volatile boolean enterStateFlag = false;
	private volatile boolean exitStateFlag = false;

	public MetricsSvcTask(
		final LoadMonitor loadMonitor, final String stepName, final int metricsPeriodSec,
		final boolean volatileOutputFlag, final Int2IntMap driversCountMap,
		final Int2IntMap concurrencyMap, final Int2ObjectMap<IoStats> ioStats,
		final Int2ObjectMap<IoStats.Snapshot> lastStats, final Int2ObjectMap<IoStats> medIoStats,
		final Int2ObjectMap<IoStats.Snapshot> lastMedStats,
		final Int2ObjectMap<SizeInBytes> itemSizeMap,
		final int activeTasksThreshold
	) throws RemoteException {
		super(loadMonitor.getSvcTasks());
		this.stepName = stepName;
		this.metricsPeriodNanoSec = TimeUnit.SECONDS.toNanos(
			metricsPeriodSec > 0 ? metricsPeriodSec : Long.MAX_VALUE
		);
		this.prevNanoTimeStamp = -1;
		this.volatileOutputFlag = volatileOutputFlag;
		this.ioStats = ioStats;
		this.lastStats = lastStats;
		this.medIoStats = medIoStats;
		this.lastMedStats = lastMedStats;
		this.concurrencyMap = concurrencyMap;
		this.driversCountMap = driversCountMap;
		this.itemSizeMap = itemSizeMap;

		this.loadMonitor = loadMonitor;
		this.activeTasksThreshold = activeTasksThreshold;
	}

	@Override
	protected final void invoke() {
		if(exclusiveInvocationLock.tryLock()) {
			try(
				final CloseableThreadContext.Instance ctx = CloseableThreadContext.put(
					KEY_STEP_NAME, stepName
				)
			) {
				nextNanoTimeStamp = nanoTime();
				if(LoadMonitor.STATS_REFRESH_PERIOD_NANOS > nextNanoTimeStamp - prevNanoTimeStamp) {
					return;
				}
				IoStats.refreshLastStats(ioStats, lastStats);
				if(nextNanoTimeStamp - prevNanoTimeStamp > metricsPeriodNanoSec) {
					IoStats.outputLastStats(
						lastStats, driversCountMap, concurrencyMap, stepName, volatileOutputFlag
					);
					
					if(medIoStats != null && !exitStateFlag) {
						if(loadMonitor.getActiveTaskCount() >= activeTasksThreshold) {
							if(!enterStateFlag) {
								Loggers.MSG.info(
									"{}: The threshold of {} active tasks count is reached, " +
										"starting the additional metrics accounting",
									stepName, activeTasksThreshold
								);
								for(final int originCode : medIoStats.keySet()) {
									medIoStats.get(originCode).start();
								}
								enterStateFlag = true;
							}
							IoStats.refreshLastStats(medIoStats, lastMedStats);
							IoStats.outputLastMedStats(
								lastMedStats, driversCountMap, concurrencyMap, stepName,
								volatileOutputFlag
							);
						}
					} else if(enterStateFlag) {
						Loggers.MSG.info(
							"{}: The active tasks count is below the threshold of {}, " +
								"stopping the additional metrics accounting",
							stepName, activeTasksThreshold
						);
						Loggers.METRICS_THRESHOLD_FILE_TOTAL.info(
							new MetricsCsvLogMessage(lastMedStats, concurrencyMap, driversCountMap)
						);
						Loggers.METRICS_THRESHOLD_EXT_RESULTS_FILE.info(
							new ExtResultsXmlLogMessage(
								stepName, lastMedStats, itemSizeMap, concurrencyMap, driversCountMap
							)
						);
						for(final int originCode : medIoStats.keySet()) {
							try {
								medIoStats.get(originCode).close();
							} catch(final IOException ignore) {
							}
						}
						medIoStats.clear();
						lastMedStats.clear();
						exitStateFlag = true;
					}
					
					prevNanoTimeStamp = nextNanoTimeStamp;
				}
			} finally {
				exclusiveInvocationLock.unlock();
			}
		}
	}

	@Override
	protected final void doClose() {
		// just wait while invocation ends and forbid any further invocation
		try {
			exclusiveInvocationLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} catch(final InterruptedException e) {
			LogUtil.exception(
				Level.WARN, e, "{}: interrupted while closing the service task",
				stepName
			);
		}
	}
}
