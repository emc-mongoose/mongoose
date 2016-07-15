package com.emc.mongoose.generator;

import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.model.api.item.Item;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 04.04.16.
 */
public class RateThrottle<T extends Item>
implements Throttle<T> {
	//
	private final long tgtNanoTime;
	//
	public RateThrottle(final float rateLimit) {
		this.tgtNanoTime = rateLimit > 0 && !Float.isInfinite(rateLimit) && !Float.isNaN(rateLimit) ?
			(long) (TimeUnit.SECONDS.toNanos(1) / rateLimit) :
			0;
	}
	//
	@Override
	public final boolean requestContinueFor(final T item)
	throws InterruptedException {
		if(tgtNanoTime > 0) {
			TimeUnit.NANOSECONDS.sleep(tgtNanoTime);
		}
		return true;
	}
	//
	@Override
	public final boolean requestContinueFor(final T item, final int times)
	throws InterruptedException {
		if(tgtNanoTime > 0 && times > 0) {
			TimeUnit.NANOSECONDS.sleep(tgtNanoTime * times);
		}
		return true;
	}
}
