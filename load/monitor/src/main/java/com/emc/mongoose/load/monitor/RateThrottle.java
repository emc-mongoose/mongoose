package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.Throttle;

/**
 Created by kurila on 04.04.16.
 */
public class RateThrottle<X>
implements Throttle<X> {

	private static final int MILLIS_IN_SEC = 1_000;

	private final long periodMillis;
	private final int countPerTime;
	private volatile int lastCount;

	public RateThrottle(final double rateLimit) {

		if(rateLimit <= 0) {
			throw new IllegalArgumentException(
				"Rate limit should be more than 0, but got " + rateLimit
			);
		}

		if(rateLimit > MILLIS_IN_SEC) {
			periodMillis = 1;
			countPerTime = (int) (rateLimit / MILLIS_IN_SEC);
		} else {
			periodMillis = (long) (MILLIS_IN_SEC / rateLimit);
			countPerTime = 1;
		}

		lastCount = countPerTime;
	}

	@Override
	public final boolean getPassFor(final X item)
	throws InterruptedException {
		synchronized(this) {
			if(lastCount == 0) {
				Thread.sleep(periodMillis);
				lastCount = countPerTime - 1;
			} else {
				lastCount --;
			}
		}
		return true;
	}
	
	@Override
	public final int getPassFor(final X item, final int times)
	throws InterruptedException {
		synchronized(this) {
			if(lastCount == 0) {
				Thread.sleep(periodMillis);
				lastCount = countPerTime;
			}
			if(times > lastCount) {
				final int n = lastCount;
				lastCount = 0;
				return n;
			} else {
				lastCount -= times;
				return times;
			}
		}
	}
}
