package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.Throttle;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 04.04.16.
 */
public class RateThrottle<X>
implements Throttle<X> {

	public static final int MIN_WAIT_NANO_TIME = 1_000;
	public static final long MAX_WAIT_BATCH_NANO_TIME = 1_000_000_000;

	private final long tgtNanoTime;
	private final int maxTimes;
	private volatile long lastNanoTime = 0;
	
	public RateThrottle(final double rateLimit) {
		this.tgtNanoTime = rateLimit > 0 && Double.isFinite(rateLimit) ?
			(long) (TimeUnit.SECONDS.toNanos(1) / rateLimit) :
			0;
		this.maxTimes = (int) (MAX_WAIT_BATCH_NANO_TIME / tgtNanoTime);
	}

	private void nanoSleep(final long nanoTime) {
		final long start = System.nanoTime();
		long remainingNanoTime = nanoTime;
		while(remainingNanoTime > MIN_WAIT_NANO_TIME) {
			LockSupport.parkNanos(remainingNanoTime);
			remainingNanoTime = nanoTime - System.nanoTime() + start;
		}
	}
	
	@Override
	public final boolean getPassFor(final X item) {
		synchronized(this) {
			long d1 = System.nanoTime() - lastNanoTime;
			long d2;
			do {
				d2 = tgtNanoTime - d1;
				if(d2 > MIN_WAIT_NANO_TIME) {
					nanoSleep(d2);
				} else {
					break;
				}
				d1 = System.nanoTime() - lastNanoTime;
			} while(true);
			lastNanoTime = System.nanoTime();
		}
		return true;
	}
	
	@Override
	public final int getPassFor(final X item, final int times) {
		final int t;
		if(times > maxTimes) {
			t = maxTimes;
		} else {
			t = times;
		}
		if(tgtNanoTime > 0 && t > 0) {
			final long reqWaitNanoTime = tgtNanoTime * t;
			synchronized(this) {
				long d1 = System.nanoTime() - lastNanoTime;
				long d2;
				do {
					d2 = reqWaitNanoTime - d1;
					if(d2 > MIN_WAIT_NANO_TIME) {
						nanoSleep(d2);
					} else {
						break;
					}
					d1 = System.nanoTime() - lastNanoTime;
				} while(true);
				lastNanoTime = System.nanoTime();
			}
		}
		return t;
	}
}
