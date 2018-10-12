package com.emc.mongoose.metrics.util;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static java.lang.Math.exp;

public class RateMeterImpl<S extends SingleMetricSnapshot>
	implements RateMeter<S> {

	private static final long TICK_INTERVAL = TimeUnit.SECONDS.toMillis(1);
	//
	private final String metricName;
	private final EWMA rateAvg;
	private final LongAdder count = new LongAdder();
	private final Clock clock;
	private final AtomicLong lastTick = new AtomicLong();
	private long startTime;

	//
	public RateMeterImpl(final Clock clock, final int periodSec, final String name) {
		final double ps = periodSec > 0 ? periodSec : 10;
		final int intervalSecs = 1;
		rateAvg = new EWMA(1 - exp(- intervalSecs / ps), intervalSecs, TimeUnit.SECONDS);
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

	@Override
	public void mark() {
		mark(1);
	}

	@Override
	public void mark(final long n) {
		tickIfNecessary();
		count.add(n);
		rateAvg.update(n);
	}

	private void tickIfNecessary() {
		final long oldTick = lastTick.get();
		final long newTick = clock.millis();
		final long age = newTick - oldTick;
		final long newIntervalStartTick = newTick - age % TICK_INTERVAL;
		if(age > TICK_INTERVAL & lastTick.compareAndSet(oldTick, newIntervalStartTick)) {
			final long requiredTicks = age / TICK_INTERVAL;
			for(long i = 0; i < requiredTicks; ++ i) {
				rateAvg.tick();
			}
		}
	}

	@Override
	public String name() {
		return metricName;
	}

	@Override
	public long count() {
		return count.sum();
	}

	@Override
	public S snapshot() {
		return (S) new RateMetricSnapshotImpl(lastRate(), meanRate(), metricName, count());
	}

	@Override
	public long elapsedTimeMillis() {
		return (clock.millis() - startTime);
	}

	@Override
	public double meanRate() {
		if(count.sum() == 0) {
			return 0.0;
		} else {
			final double elapsed = (clock.millis() - startTime);
			return count.sum() / elapsed * TimeUnit.SECONDS.toNanos(1);
		}
	}

	@Override
	public double lastRate() {
		tickIfNecessary();
		return rateAvg.rate(TimeUnit.SECONDS);
	}
}
