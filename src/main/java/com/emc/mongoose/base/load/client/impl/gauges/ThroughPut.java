package com.emc.mongoose.base.load.client.impl.gauges;
//
import com.codahale.metrics.Gauge;
//
import com.emc.mongoose.base.load.client.impl.tasks.GaugeValueTask;
import com.emc.mongoose.util.logging.TraceLogger;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
/**
 Created by kurila on 19.12.14.
 */
public class ThroughPut
implements Gauge<Double> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final ExecutorService mgmtConnExecutor;
	//
	private final GaugeValueTask<Double> taskGetBW;
	private final GaugeValueTask<Long> taskGetCountSucc, taskGetCountBytes;
	//
	private Future<Double> futureBW;
	private Future<Long> futureCountSucc, futureCountBytes;
	//
	public ThroughPut(
		final ExecutorService mgmtConnExecutor, final GaugeValueTask<Double> taskGetBW,
		final GaugeValueTask<Long> taskGetCountSucc, final GaugeValueTask<Long> taskGetCountBytes
	) {
		this.mgmtConnExecutor = mgmtConnExecutor;
		this.taskGetBW = taskGetBW;
		this.taskGetCountSucc = taskGetCountSucc;
		this.taskGetCountBytes = taskGetCountBytes;
	}
	//
	@Override
	public final Double getValue() {
		double x = 0, y = 0;
		try {
			futureBW = mgmtConnExecutor.submit(taskGetBW);
			futureCountSucc = mgmtConnExecutor.submit(taskGetCountSucc);
			futureCountBytes = mgmtConnExecutor.submit(taskGetCountBytes);
			try {
				x = futureBW.get();
				x *= futureCountSucc.get();
				y = futureCountBytes.get();
			} catch(final InterruptedException | ExecutionException e) {
				TraceLogger.failure(LOG, Level.DEBUG, e, "Metric value fetching failed");
			}
		} catch(final RejectedExecutionException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Metric value fetching failed due to reject");
		}
		return y == 0 ? 0 : x / y;
	}
}
