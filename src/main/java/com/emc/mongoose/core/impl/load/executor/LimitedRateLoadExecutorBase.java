package com.emc.mongoose.core.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.RequestConfig;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 04.05.15.
 The extension of load executor which is able to sustain the rate (throughput, item/sec) not higher
 than the specified limit.
 */
public abstract class LimitedRateLoadExecutorBase<T extends Item>
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
		final ItemSrc<T> itemSrc, final long maxCount,
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
	@Override
	public Future<? extends IOTask<T>> submitReq(final IOTask<T> request)
	throws RejectedExecutionException {
		// manual delay
		if(manualTaskSleepMicroSecs > 0) {
			try {
				TimeUnit.MILLISECONDS.sleep(
					TimeUnit.MICROSECONDS.toMillis(manualTaskSleepMicroSecs)
				);
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted request sleep");
			}
		}
		// rate limit matching
		if(rateLimit > 0 && lastStats.getSuccRateLast() > rateLimit) {
			final int microDelay = (int) (
				tgtDurMicroSecs - lastStats.getDurationSum() / lastStats.getSuccCount()
			);
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Next delay: {}[us]", microDelay);
			}
			try {
				TimeUnit.MICROSECONDS.sleep(microDelay);
			} catch(final InterruptedException e) {
				throw new RejectedExecutionException(e);
			}
		}
		//
		return submitReqActually(request);
	}
	//
	protected abstract Future<? extends IOTask<T>> submitReqActually(final IOTask<T> request)
	throws RejectedExecutionException;
}
