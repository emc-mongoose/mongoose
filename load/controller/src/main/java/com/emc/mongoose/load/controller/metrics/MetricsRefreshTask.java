package com.emc.mongoose.load.controller.metrics;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.concurrent.SvcTaskBase;
import com.emc.mongoose.load.monitor.ExtResultsXmlLogMessage;
import com.emc.mongoose.model.load.LoadController;
import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;
import com.emc.mongoose.ui.log.Loggers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by andrey on 31.03.17.
 */
public class MetricsRefreshTask
extends SvcTaskBase {
	
	private static final String CLASS_NAME = MetricsRefreshTask.class.getSimpleName();
	
	private final Lock exclusiveInvocationLock = new ReentrantLock();
	private final String stepName;
	private final Int2ObjectMap<MetricsContext> ioStats;
	private final Int2ObjectMap<MetricsContext> thresholdIoStats;
	private final Int2ObjectMap<SizeInBytes> itemSizeMap;
	private final LoadController loadController;
	private final int activeTasksThreshold;

	private volatile boolean enterStateFlag = false;
	private volatile boolean exitStateFlag = false;

	public MetricsRefreshTask(
		final LoadController loadController,
		final Int2ObjectMap<MetricsContext> ioStats, final Int2ObjectMap<MetricsContext> thresholdIoStats,
		final Int2ObjectMap<SizeInBytes> itemSizeMap, final int activeTasksThreshold
	) throws RemoteException {
		super(loadController.getSvcTasks());
		this.stepName = loadController.getName();
		this.ioStats = ioStats;
		this.thresholdIoStats = thresholdIoStats;
		this.itemSizeMap = itemSizeMap;

		this.loadController = loadController;
		this.activeTasksThreshold = activeTasksThreshold;
	}

	@Override
	protected final void invoke() {
		if(exclusiveInvocationLock.tryLock()) {
			try(
				final CloseableThreadContext.Instance ctx = CloseableThreadContext
					.put(KEY_STEP_NAME, stepName)
					.put(KEY_CLASS_NAME, CLASS_NAME)
			) {
				MetricsContext ioTypeStats;
				for(final int nextIoTypeCode : ioStats.keySet()) {
					ioTypeStats = ioStats.get(nextIoTypeCode);
					if(ioTypeStats != null) {
						ioTypeStats.refreshLastSnapshot();
					}
				}
				
				if(thresholdIoStats != null && !exitStateFlag) {
					if(loadController.getActiveTaskCount() >= activeTasksThreshold) {
						if(!enterStateFlag) {
							Loggers.MSG.info(
								"The threshold of {} active tasks count is reached, " +
									"starting the additional metrics accounting",
								activeTasksThreshold
							);
							for(final int originCode : thresholdIoStats.keySet()) {
								thresholdIoStats.get(originCode).start();
							}
							enterStateFlag = true;
						}
						for(final int nextIoTypeCode : thresholdIoStats.keySet()) {
							ioTypeStats = thresholdIoStats.get(nextIoTypeCode);
							ioTypeStats.refreshLastSnapshot();
						}
					}
				} else if(enterStateFlag) {
					Loggers.MSG.info(
						"The active tasks count is below the threshold of {}, " +
							"stopping the additional metrics accounting", activeTasksThreshold
					);
					Loggers.METRICS_THRESHOLD_FILE_TOTAL.info(
						new MetricsCsvLogMessage(thresholdIoStats)
					);
					/*Loggers.METRICS_THRESHOLD_EXT_RESULTS_FILE.info(
						new ExtResultsXmlLogMessage(thresholdIoStats, itemSizeMap)
					);*/
					for(final int originCode : thresholdIoStats.keySet()) {
						try {
							thresholdIoStats.get(originCode).close();
						} catch(final IOException ignore) {
						}
					}
					thresholdIoStats.clear();
					exitStateFlag = true;
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
