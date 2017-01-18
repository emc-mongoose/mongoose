package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import static java.lang.System.nanoTime;

/**
 Created by kurila on 04.04.16.
 */
public final class RateThrottle<X>
implements Throttle<X> {

	private final static Logger LOG = LogManager.getLogger();

	private final long periodNanos;
	private volatile long startTime = -1;
	private volatile long acquiredCount = 0;

	public RateThrottle(final double rateLimit) {
		if(rateLimit <= 0) {
			throw new IllegalArgumentException(
				"Rate limit should be more than 0, but got " + rateLimit
			);
		}
		periodNanos = (long) (TimeUnit.SECONDS.toNanos(1) / rateLimit);
		LOG.info(
			Markers.MSG, "Rate limit throttle is configured to pass the request each {}[ns]",
			periodNanos
		);
	}

	@Override
	public final boolean getPassFor(final X item) {
		synchronized(this) {
			if(startTime > 0) {
				final long periodCount = (nanoTime() - startTime) / periodNanos;
				if(periodCount > acquiredCount) {
					acquiredCount ++;
					return true;
				} else {
					return false;
				}
			} else {
				startTime = nanoTime();
				acquiredCount ++;
				return true;
			}
		}
	}
	
	@Override
	public final int getPassFor(final X item, final int requiredCount) {
		synchronized(this) {
			if(startTime > 0) {
				final int availableCount = (int) (
					(nanoTime() - startTime) / periodNanos - acquiredCount
				);
				if(availableCount > requiredCount) {
					acquiredCount += requiredCount;
					return requiredCount;
				} else {
					acquiredCount += availableCount;
					return availableCount;
				}
			} else {
				startTime = nanoTime();
				acquiredCount += requiredCount;
				return requiredCount;
			}
		}
	}
}
