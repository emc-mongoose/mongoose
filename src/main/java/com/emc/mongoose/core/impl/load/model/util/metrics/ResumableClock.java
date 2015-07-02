package com.emc.mongoose.core.impl.load.model.util.metrics;

import com.codahale.metrics.Clock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gusakk on 02.07.15.
 */
public class ResumableClock extends Clock {
	//
	//  these parameters are necessary for pause/resume Mongoose w/ SIGSTOP and SIGCONT signals
	private long lastTimeBeforeTermination;
	private long elapsedTimeInPause;
	private static final long TICK_INTERVAL = TimeUnit.SECONDS.toNanos(5);
	private AtomicBoolean wasPaused;
	//
	public ResumableClock() {
		lastTimeBeforeTermination = 0;
		elapsedTimeInPause = 0;
		wasPaused = new AtomicBoolean(false);
	}
	//
	@Override
	public long getTick() {
		//  This Clock's implementation provides correct time for calculating different metrics
		//  after resumption Mongoose's run w/ SIGCONT signal.
		final long currTime = System.nanoTime();
		if (lastTimeBeforeTermination > 0) {
			if (currTime - lastTimeBeforeTermination > TICK_INTERVAL) {
				elapsedTimeInPause += currTime - lastTimeBeforeTermination;
				lastTimeBeforeTermination = currTime;
				return currTime - elapsedTimeInPause;
			}
		}
		lastTimeBeforeTermination = currTime;
		return currTime - elapsedTimeInPause;
	}
	//
	public boolean wasPaused() {
		if (System.nanoTime() - lastTimeBeforeTermination > TICK_INTERVAL) {
			wasPaused.set(true);
		}
		return wasPaused.get();
	}
}
