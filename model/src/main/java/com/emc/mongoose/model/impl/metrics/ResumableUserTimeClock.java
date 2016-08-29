package com.emc.mongoose.model.impl.metrics;

import com.codahale.metrics.Clock;

import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 02.07.15.
 */
public class ResumableUserTimeClock
extends Clock.UserTimeClock {
	//
	//  these parameters are necessary for pause/resume Mongoose w/ SIGSTOP and SIGCONT signals
	private long lastTimeBeforeTermination;
	private long elapsedTimeInPause;
	private static final long TICK_INTERVAL = TimeUnit.SECONDS.toNanos(1);
	//
	public ResumableUserTimeClock() {
		lastTimeBeforeTermination = 0;
		elapsedTimeInPause = 0;
	}
	//
	@Override
	public long getTick() {
		//  TODO: implement this functionality #JIRA-451
		//  This Clock's implementation provides correct time for calculating different metrics
		//  after resumption Mongoose's run w/ SIGCONT signal.
		/*final long currTime = System.nanoTime();
		if(lastTimeBeforeTermination > 0) {
			if(currTime - lastTimeBeforeTermination > TICK_INTERVAL) {
				elapsedTimeInPause += currTime - lastTimeBeforeTermination;
				lastTimeBeforeTermination = currTime;
				return currTime - elapsedTimeInPause;
			}
		}
		lastTimeBeforeTermination = currTime;
		return currTime - elapsedTimeInPause;*/
		return super.getTick();
	}
}
