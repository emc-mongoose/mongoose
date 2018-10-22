package com.emc.mongoose.metrics.type;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static java.lang.Math.exp;

/**
 @author veronika K. on 26.09.18 */
public class EWMA
implements LoadAverage {

	private static final int INTERVAL = 5;
	private static final double SECONDS_PER_MINUTE = 60.0;
	private static final int ONE_MINUTE = 1;
	private static final int FIVE_MINUTES = 5;
	private static final int FIFTEEN_MINUTES = 15;
	private static final double M1_ALPHA = 1 - exp(- INTERVAL / SECONDS_PER_MINUTE / ONE_MINUTE);
	private static final double M5_ALPHA = 1 - exp(- INTERVAL / SECONDS_PER_MINUTE / FIVE_MINUTES);
	private static final double M15_ALPHA = 1 - exp(- INTERVAL / SECONDS_PER_MINUTE / FIFTEEN_MINUTES);
	private volatile boolean initialized = false;
	private volatile double rate = 0.0;
	private final LongAdder uncounted = new LongAdder();
	private final double alpha, interval;

	/**
	 Creates a new EWMA which is equivalent to the UNIX one minute load average and which expects
	 to be ticked every 5 seconds.

	 @return a one-minute EWMA
	 */
	public static EWMA oneMinuteEWMA() {
		return new EWMA(M1_ALPHA, INTERVAL, TimeUnit.SECONDS);
	}

	/**
	 Creates a new EWMA which is equivalent to the UNIX five minute load average and which expects
	 to be ticked every 5 seconds.

	 @return a five-minute EWMA
	 */
	public static EWMA fiveMinuteEWMA() {
		return new EWMA(M5_ALPHA, INTERVAL, TimeUnit.SECONDS);
	}

	/**
	 Creates a new EWMA which is equivalent to the UNIX fifteen minute load average and which
	 expects to be ticked every 5 seconds.

	 @return a fifteen-minute EWMA
	 */
	public static EWMA fifteenMinuteEWMA() {
		return new EWMA(M15_ALPHA, INTERVAL, TimeUnit.SECONDS);
	}

	/**
	 Create a new EWMA with a specific smoothing constant.

	 @param alpha the smoothing constant
	 @param interval the expected tick interval
	 @param intervalUnit the time unit of the tick interval
	 */
	public EWMA(final double alpha,final long interval,final TimeUnit intervalUnit) {
		this.interval = intervalUnit.toNanos(interval);
		this.alpha = alpha;
	}

	@Override
	public void update(final long n) {
		uncounted.add(n);
	}

	@Override
	public void tick() {
		final long count = uncounted.sumThenReset();
		final double instantRate = count / interval;
		if(initialized) {
			final double oldRate = this.rate;
			rate = oldRate + (alpha * (instantRate - oldRate));
		} else {
			rate = instantRate;
			initialized = true;
		}
	}

	@Override
	public double rate(final TimeUnit rateUnit) {
		return rate * (double) rateUnit.toNanos(1);
	}
}
