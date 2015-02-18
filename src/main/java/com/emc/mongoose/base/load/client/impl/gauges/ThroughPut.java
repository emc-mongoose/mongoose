package com.emc.mongoose.base.load.client.impl.gauges;
//
import com.codahale.metrics.Gauge;
//
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.threading.PeriodicTask;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 19.12.14.
 */
public class ThroughPut
implements Gauge<Double> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final PeriodicTask<Double> taskGetBW;
	private final PeriodicTask<Long> taskGetCountSucc, taskGetCountBytes;
	//
	public ThroughPut(
		final PeriodicTask<Double> taskGetBW, final PeriodicTask<Long> taskGetCountSucc,
		final PeriodicTask<Long> taskGetCountBytes
	) {
		this.taskGetBW = taskGetBW;
		this.taskGetCountSucc = taskGetCountSucc;
		this.taskGetCountBytes = taskGetCountBytes;
	}
	//
	private double x = 0, y = 0;
	//
	@Override
	public final Double getValue() {
		try {
			x = taskGetBW.getLastResult();
			x *= taskGetCountSucc.getLastResult();
			y = taskGetCountBytes.getLastResult();
		} catch(final NullPointerException e) {
			if(LOG.isTraceEnabled(Markers.ERR)) {
				TraceLogger.failure(LOG, Level.TRACE, e, "Some values are not initialized yet");
			}
		}
		return y == 0 ? 0 : x / y;
	}
}
