package com.emc.mongoose.core.impl.load.tasks;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.common.log.appenders.WebSocketLogListener;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.load.tasks.processors.ChartPackage;
import com.emc.mongoose.core.impl.load.tasks.processors.PolylineManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 08.12.15.
 */
public final class LogMetricsTask
implements Runnable {
	//
	public static List<WebSocketLogListener> LISTENERS;
	//
	public static void setListeners(List<WebSocketLogListener> listeners) {
		LogMetricsTask.LISTENERS = listeners;
	}
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadExecutor loadExecutor;
	private final int metricsPeriodSec;
	private String runId;
	//
	public LogMetricsTask(final LoadExecutor loadExecutor, final int metricsPeriodSec, String runId) {
		this.runId = runId;
		this.loadExecutor = loadExecutor;
		this.metricsPeriodSec = metricsPeriodSec;
	}
	//
	@Override
	public final
	void run() {
		final Thread currThread = Thread.currentThread();
		PolylineManager polylineManager = new PolylineManager();
		String loadJobName = null;
		try {
			loadJobName = loadExecutor.getName();
			ThreadContext.put(LogUtil.LOAD_JOB_NAME, loadJobName);
		} catch (RemoteException ignored) {
		}
		try {
			currThread.setName(loadExecutor.getName() + "-metrics");
			try {
				while(!currThread.isInterrupted()) {
					LOG.info(Markers.PERF_AVG, loadExecutor.getStatsSnapshot());
					polylineManager.updatePolylines(loadExecutor.getStatsSnapshot());
					sendCharts(loadJobName, polylineManager);
					Thread.yield(); TimeUnit.SECONDS.sleep(metricsPeriodSec);
				}
			} catch(final InterruptedException e) {
				LOG.debug(Markers.MSG, "{}: interrupted", loadExecutor.getName());
			}
		} catch(final RemoteException ignored) {
		}
	}

	private void sendCharts(String loadJobName, PolylineManager polylineManager) {
		for (WebSocketLogListener listener: LISTENERS) {
				listener.sendMessage(new ChartPackage(runId, loadJobName, polylineManager));
		}
	}
}
