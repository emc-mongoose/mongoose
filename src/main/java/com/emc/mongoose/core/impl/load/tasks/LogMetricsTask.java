package com.emc.mongoose.core.impl.load.tasks;
//
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 08.12.15.
 */
public final class LogMetricsTask
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static String LOAD_JOB_NAME = "loadJobName";
	//
	private final LoadExecutor loadExecutor;
	private final int metricsPeriodSec;
	//
	public LogMetricsTask(final LoadExecutor loadExecutor, final int metricsPeriodSec) {
		this.loadExecutor = loadExecutor;
		this.metricsPeriodSec = metricsPeriodSec;
	}
	//
	@Override
	public final
	void run() {
		final Thread currThread = Thread.currentThread();
		try {
			ThreadContext.put(LOAD_JOB_NAME, loadExecutor.getName());
		} catch (RemoteException ignored) {;
		}
		try {
			currThread.setName(loadExecutor.getName() + "-metrics");
			try {
				while(!currThread.isInterrupted()) {
					loadExecutor.logMetrics(Markers.PERF_AVG);
					Thread.yield(); TimeUnit.SECONDS.sleep(metricsPeriodSec);
				}
			} catch(final InterruptedException e) {
				LOG.debug(Markers.MSG, "{}: interrupted", loadExecutor.getName());
			}
		} catch(final RemoteException ignored) {
		}
	}
}
