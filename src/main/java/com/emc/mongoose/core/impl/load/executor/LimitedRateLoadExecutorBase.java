package com.emc.mongoose.core.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
//
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
	private final int tgtDurMicroSecs;
	//
	protected LimitedRateLoadExecutorBase(
		final AppConfig appConfig,
		final IOConfig<? extends DataItem, ? extends Container<? extends DataItem>> ioConfig,
		final String[] addrs, final int threadCount, final ItemSrc<T> itemSrc, final long maxCount,
		final float rateLimit
	) throws ClassCastException {
		super(appConfig, ioConfig, addrs, threadCount, itemSrc, maxCount);
		//
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Frequency rate limit shouldn't be a negative value");
		}
		this.rateLimit = rateLimit;
		if(rateLimit > 0) {
			tgtDurMicroSecs = (int) (1000000 * totalThreadCount / rateLimit);
			LOG.debug(
				Markers.MSG, "{}: target I/O task durations is {}[us]", getName(), tgtDurMicroSecs
			);
		} else {
			tgtDurMicroSecs = 0;
		}
	}
	//
	@Override
	public <A extends IOTask<T>> Future<A> submitReq(final A request)
	throws RejectedExecutionException {
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
		return submitTaskActually(request);
	}
	//
	protected abstract <A extends IOTask<T>> Future<A> submitTaskActually(final A ioTask)
	throws RejectedExecutionException;
}
