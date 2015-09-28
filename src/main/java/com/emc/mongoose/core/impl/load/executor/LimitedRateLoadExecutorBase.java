package com.emc.mongoose.core.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.io.req.RequestConfig;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.List;
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
	private final int manualTaskSleepMicroSecs, tgtDurMicroSecs;
	//
	protected LimitedRateLoadExecutorBase(
		final RunTimeConfig runTimeConfig, final RequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final int threadCount,
		final DataItemSrc<T> itemSrc, final long maxCount,
		final int manualTaskSleepMicroSecs, final float rateLimit
	) throws ClassCastException {
		super(runTimeConfig, reqConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount);
		//
		this.manualTaskSleepMicroSecs = manualTaskSleepMicroSecs;
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Frequency rate limit shouldn't be a negative value");
		}
		this.rateLimit = rateLimit;
		if(rateLimit > 0) {
			tgtDurMicroSecs = (int) (1000000 * totalConnCount / rateLimit);
			LOG.debug(
				Markers.MSG, "{}: target I/O task durations is {}[us]", getName(), tgtDurMicroSecs
			);
		} else {
			tgtDurMicroSecs = 0;
		}
	}
	//
	private void invokeDelayToMatchRate(final int itemCountToFeed)
	throws InterruptedException {
		if(rateLimit > 0 && lastStats.getSuccRateLast() > rateLimit && itemCountToFeed > 0) {
			final int microDelay = itemCountToFeed * (int) (
				tgtDurMicroSecs - lastStats.getDurationSum() / lastStats.getSuccRateMean()
			);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Next delay: {}[us]", microDelay);
			}
			TimeUnit.MICROSECONDS.sleep(microDelay);
		}
	}
	/**
	 Adds the optional delay calculated from last successful I/O task duration and the target
	 duration
	 */
	@Override
	public void put(final T dataItem)
	throws InterruptedException, RemoteException, RejectedExecutionException {
		if(manualTaskSleepMicroSecs > 0) {
			try {
				TimeUnit.MILLISECONDS.sleep(manualTaskSleepMicroSecs);
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted request sleep");
			}
		}
		invokeDelayToMatchRate(1);
		super.put(dataItem);
	}
	/**
	 Adds the optional delay calculated from last successful I/O task duration and the target
	 duration
	 */
	@Override
	public int put(final List<T> dataItems, final int from, final int to)
	throws InterruptedException, RemoteException, RejectedExecutionException {
		final int n = to - from;
		if(n > 0) {
			if(manualTaskSleepMicroSecs > 0) {
				try {
					TimeUnit.MICROSECONDS.sleep(n * manualTaskSleepMicroSecs);
				} catch(final InterruptedException e) {
					LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted request sleep");
				}
			}
			invokeDelayToMatchRate(n);
			return super.put(dataItems, from, to);
		} else {
			return 0;
		}
	}
}
