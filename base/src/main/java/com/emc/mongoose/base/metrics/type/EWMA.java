package com.emc.mongoose.base.metrics.type;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
* Copied from dropwizard metrics library 4.0.3
*
* @author veronika K. on 26.09.18
*/
public final class EWMA implements LoadAverage {

	private volatile boolean initialized = false;
	private volatile double rate = 0.0;
	private final LongAdder uncounted = new LongAdder();
	private final double alpha, interval;

	/**
	* Create a new EWMA with a specific smoothing constant.
	*
	* @param alpha the smoothing constant
	* @param interval the expected tick interval
	* @param intervalUnit the time unit of the tick interval
	*/
	public EWMA(final double alpha, final long interval, final TimeUnit intervalUnit) {
		this.interval = intervalUnit.toNanos(interval);
		this.alpha = alpha;
	}

	@Override
	public final void update(final long n) {
		uncounted.add(n);
	}

	@Override
	public final void tick() {
		final long count = uncounted.sumThenReset();
		final double instantRate = count / interval;
		if (initialized) {
			final double oldRate = this.rate;
			rate = oldRate + (alpha * (instantRate - oldRate));
		} else {
			rate = instantRate;
			initialized = true;
		}
	}

	@Override
	public final double rate(final TimeUnit rateUnit) {
		return rate * (double) rateUnit.toNanos(1);
	}
}
