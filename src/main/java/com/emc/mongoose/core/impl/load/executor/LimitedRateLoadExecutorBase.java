package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 04.05.15.
 The extension of load executor which is able to sustain the rate (throughput, item/sec) not higher
 than the specified limit.
 */
public abstract class LimitedRateLoadExecutorBase<T extends DataItem>
extends LoadExecutorBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final float rateLimit;
	private final int tgtDur;
	//
	protected LimitedRateLoadExecutorBase(
		final Class<T> dataCls,
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias,
		final float rateLimit
	) throws ClassCastException {
		super(
			dataCls,
			runTimeConfig, reqConfig, addrs, connCountPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias
		);
		//
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Frequency rate limit shouldn't be a negative value");
		}
		this.rateLimit = rateLimit;
		if(rateLimit > 0) {
			tgtDur = (int) (1000000 * addrs.length * connCountPerNode / rateLimit);
			LOG.debug(Markers.MSG, "{}: target I/O task durations is {}[us]", getName(), tgtDur);
		} else {
			tgtDur = 0;
		}
	}
	/**
	 Adds the optional delay calculated from last successful I/O task duration and the target
	 duration
	 */
	@Override
	public void submit(final T dataItem)
	throws InterruptedException, RemoteException, RejectedExecutionException {
		if(rateLimit > 0 && throughPut.getMeanRate() > rateLimit) {
			final int microDelay = (int) (tgtDur - durTasksSum.get() / throughPut.getCount());
			if(microDelay > 0) {
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, "Next delay: {}[us]", microDelay);
				}
				TimeUnit.MICROSECONDS.sleep(microDelay);
			}
		}
		super.submit(dataItem);
	}
}
