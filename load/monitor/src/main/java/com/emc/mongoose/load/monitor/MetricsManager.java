package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.model.DaemonBase;
import com.emc.mongoose.model.metrics.MetricsContext;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;
import static com.emc.mongoose.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.common.Constants.KEY_STEP_NAME;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import java.rmi.RemoteException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by kurila on 18.05.17.
 */
public final class MetricsManager
extends DaemonBase
implements SvcTask {
	
	private final SortedSet<MetricsContext> allMetrics = new TreeSet<>();
	private final Lock allMetricsLock = new ReentrantLock();
	private final long outputPeriodMillis;
	private volatile long lastOutputTs;
	private volatile long nextOutputTs;
	
	private MetricsManager(final long outputPeriodMillis) {
		svcTasks.add(this);
		this.outputPeriodMillis = outputPeriodMillis;
		lastOutputTs = System.currentTimeMillis() - outputPeriodMillis;
	}
	
	private static final String CLASS_NAME = MetricsManager.class.getName();
	private static final MetricsManager INSTANCE;
	
	static {
		try {
			final Config defaultConfig  = ConfigParser.loadDefaultConfig();
			final long defaultPeriod = defaultConfig
				.getTestConfig()
				.getStepConfig()
				.getMetricsConfig()
				.getPeriod();
			INSTANCE = new MetricsManager(TimeUnit.SECONDS.toMillis(defaultPeriod));
			INSTANCE.start();
		} catch(final Throwable cause) {
			throw new AssertionError(cause);
		}
	}
	
	public static void register(final MetricsContext metricsCtx) {
		try {
			if(INSTANCE.allMetricsLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				try {
					if(INSTANCE.allMetrics.add(metricsCtx)) {
						Loggers.MSG.debug("Metrics context \"{}\" registered", metricsCtx);
					} else {
						Loggers.ERR.warn(
							"Metrics context \"{}\" has been registered already", metricsCtx
						);
					}
				} finally {
					INSTANCE.allMetricsLock.unlock();
				}
			} else {
				Loggers.ERR.warn("Locking timeout at register call");
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "Failed to register \"{}\"", metricsCtx);
		}
	}
	
	public static void unregister(final MetricsContext metricsCtx) {
		try {
			if(INSTANCE.allMetricsLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				try {
					if(INSTANCE.allMetrics.remove(metricsCtx)) {
						Loggers.METRICS_FILE_TOTAL.info(new MetricsCsvLogMessage(metricsCtx));
						Loggers.METRICS_STD_OUT.info(new BasicMetricsLogMessage(metricsCtx));
					} else {
						Loggers.ERR.warn(
							"Metrics context \"{}\" has not been registered",
							metricsCtx
						);
					}
				} finally {
					INSTANCE.allMetricsLock.unlock();
				}
			} else {
				Loggers.ERR.warn("Locking timeout at unregister call");
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "Failed to unregister \"{}\"", metricsCtx);
		}
	}
	
	@Override
	public final void run() {
		if(allMetricsLock.tryLock()) {
			try {
				nextOutputTs = System.currentTimeMillis();
				for(final MetricsContext metricsCtx : allMetrics) {
					metricsCtx.refreshLastSnapshot();
					if(
						nextOutputTs  - metricsCtx.getLastOutputTs() >=
							metricsCtx.getOutputPeriodMillis()
					) {
						try(
							final CloseableThreadContext.Instance logCtx = CloseableThreadContext
								.put(KEY_STEP_NAME, metricsCtx.getStepName())
						        .put(KEY_CLASS_NAME, CLASS_NAME)
						) {
							Loggers.METRICS_FILE.info(new MetricsCsvLogMessage(metricsCtx));
						}
					}
				}
				if(nextOutputTs - lastOutputTs >= outputPeriodMillis) {
					lastOutputTs = nextOutputTs;
					Loggers.METRICS_STD_OUT.info(new MetricsAsciiTableLogMessage(allMetrics));
				}
			} finally {
				allMetricsLock.unlock();
			}
		}
	}
	
	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, RemoteException {
		if(isStarted() || isShutdown()) {
			state.wait(timeUnit.toMillis(timeout));
		}
		return !(isStarted() || isShutdown());
	}
	
	@Override
	protected final void doShutdown() {
	}
	
	@Override
	protected final void doInterrupt() {
		svcTasks.remove(this);
		try { // obtain the lock to prevent further execution
			if(!allMetricsLock.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				Loggers.ERR.warn("Locking timeout at interrupt call");
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "Failed to interrupt");
		}
	}
	
	@Override
	protected final void doClose() {
		allMetrics.clear();
	}
}
