package com.emc.mongoose.metrics.type;

import com.emc.mongoose.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.RateMetricSnapshotImpl;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import static java.lang.Math.exp;

public class RateMeterImpl
implements RateMeter<RateMetricSnapshot> {

	private static final long TICK_INTERVAL = TimeUnit.SECONDS.toMillis(1);

	private final String metricName;
	private final EWMA rateAvg;
	private final LongAdder count = new LongAdder();
	private final Clock clock;
	private final AtomicLong lastTick = new AtomicLong();
	private long startTime;

	public RateMeterImpl(final Clock clock, final int periodSec, final String name) {
		final double ps = periodSec > 0 ? periodSec : 10;
		final int intervalSecs = 1;
		rateAvg = new EWMA(1 - exp(-intervalSecs / ps), intervalSecs, TimeUnit.SECONDS);
		this.clock = clock;
		startTime = clock.millis();
		lastTick.set(startTime);
		this.metricName = name;
	}

	@Override
	public void resetStartTime() {
		startTime = clock.millis();
		lastTick.set(startTime);
	}

	private void tickIfNecessary() {
		final long oldTick = lastTick.get();
		final long newTick = clock.millis();
		final long age = newTick - oldTick;
		if(age > TICK_INTERVAL) {
			final long newIntervalStartTick = newTick - age % TICK_INTERVAL;
			if(lastTick.compareAndSet(oldTick, newIntervalStartTick)) {
				final long requiredTicks = age / TICK_INTERVAL;
				for(long i = 0; i < requiredTicks; i ++) {
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
		return new RateMetricSnapshotImpl(lastRate(), meanRate(), metricName, count.sum(), elapsedTimeMillis());
	}

	long elapsedTimeMillis() {
		return (clock.millis() - startTime);
	}

	double meanRate() {
		if(count.sum() == 0) {
			return 0.0;
		} else {
			final double elapsed = TimeUnit.MILLISECONDS.toSeconds(clock.millis() - startTime);
			return (elapsed == 0) ? 0 : count.sum() / elapsed;
		}
	}

	double lastRate() {
		tickIfNecessary();
		return rateAvg.rate(TimeUnit.SECONDS);
	}
}
