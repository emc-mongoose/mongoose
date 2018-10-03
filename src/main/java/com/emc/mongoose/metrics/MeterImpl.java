package com.emc.mongoose.metrics;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static java.lang.Math.exp;

public class MeterImpl
	implements Meter {

	private static final long TICK_INTERVAL = TimeUnit.SECONDS.toMillis(1);
	//
	private final EWMA rateAvg;
	private final LongAdder count = new LongAdder();
	private final Clock clock;
	private final AtomicLong lastTick = new AtomicLong();
	private long startTime;

	//
	public MeterImpl(final Clock clock, final int periodSec) {
		final double ps = periodSec > 0 ? periodSec : 10;
		final int intervalSecs = 1;
		rateAvg = new EWMA(1 - exp(- intervalSecs / ps), intervalSecs, TimeUnit.SECONDS);
		this.clock = clock;
		startTime = clock.millis();
		lastTick.set(startTime);
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
	public void mark(long n) {
		tickIfNecessary();
		count.add(n);
		rateAvg.update(n);
	}

	private void tickIfNecessary() {
		final long oldTick = lastTick.get();
		final long newTick = clock.millis();
		final long age = newTick - oldTick;
		if(age > TICK_INTERVAL) {
			final long newIntervalStartTick = newTick - age % TICK_INTERVAL;
			if(lastTick.compareAndSet(oldTick, newIntervalStartTick)) {
				final long requiredTicks = age / TICK_INTERVAL;
				for(long i = 0; i < requiredTicks; ++ i) {
					rateAvg.tick();
				}
			}
		}
	}

	@Override
	public long count() {
		return count.sum();
	}

	@Override
	public double meanRate() {
		if(count() == 0) {
			return 0.0;
		} else {
			final double elapsed = (clock.millis() - startTime);
			return count() / elapsed * TimeUnit.SECONDS.toNanos(1);
		}
	}

	@Override
	public double lastRate() {
		tickIfNecessary();
		return rateAvg.rate(TimeUnit.SECONDS);
	}
}
