package com.emc.mongoose.base.metrics.type;

import static java.lang.Math.exp;

import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshotImpl;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class RateMeterImpl implements RateMeter<RateMetricSnapshot> {

	private static final long TICK_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(1);

	private final String metricName;
	private final LoadAverage rateAvg;
	private final LongAdder count = new LongAdder();
	private final Clock clock;
	private final AtomicLong lastTick = new AtomicLong();
	private long startTimeMillis;

	public RateMeterImpl(final Clock clock, final String name) {
		this(clock, DEFAULT_PERIOD_SECONDS, name);
	}

	protected RateMeterImpl(final Clock clock, final int period, final String name)
					throws IllegalArgumentException {
		if (period <= 0) {
			throw new IllegalArgumentException("Period should be more than 0 [s]");
		}
		final int interval = 1;
		rateAvg = new EWMA(1 - exp(-interval / (double) period), interval, TimeUnit.SECONDS);
		this.clock = clock;
		resetStartTime();
		this.metricName = name;
	}

	@Override
	public void resetStartTime() {
		startTimeMillis = clock.millis();
		lastTick.set(startTimeMillis);
	}

	private void tickIfNecessary() {
		final long oldTick = lastTick.get();
		final long newTick = clock.millis();
		final long ageMillis = newTick - oldTick;
		if (ageMillis > TICK_INTERVAL_MILLIS) {
			final long newIntervalStartTick = newTick - ageMillis % TICK_INTERVAL_MILLIS;
			if (lastTick.compareAndSet(oldTick, newIntervalStartTick)) {
				final long requiredTicks = ageMillis / TICK_INTERVAL_MILLIS;
				for (long i = 0; i < requiredTicks; ++i) {
					rateAvg.tick();
				}
			}
		}
	}

	@Override
	public void update(final long v) {
		tickIfNecessary();
		count.add(v);
		rateAvg.update(v);
	}

	@Override
	public RateMetricSnapshotImpl snapshot() {
		return new RateMetricSnapshotImpl(
						lastRate(), meanRate(), metricName, count.sum(), elapsedTimeMillis());
	}

	long elapsedTimeMillis() {
		return (clock.millis() - startTimeMillis);
	}

	double meanRate() {
		if (count.sum() == 0) {
			return 0.0;
		} else {
			final double elapsed = TimeUnit.MILLISECONDS.toSeconds(clock.millis() - startTimeMillis);
			return (elapsed == 0) ? 0 : count.sum() / elapsed;
		}
	}

	double lastRate() {
		tickIfNecessary();
		return rateAvg.rate(TimeUnit.SECONDS);
	}
}
