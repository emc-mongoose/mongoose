package com.emc.mongoose.load.monitor;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.*;

/**
 Created by andrey on 08.11.16.
 */
public class RateThrottleTest {

	@Test
	public void testRate100mHzNonBatch()
	throws Exception {
		final double rateLimit = 0.1;
		final int timeLimitSec = 100;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				while(true) {
					throttle.getPassFor(subj);
					counter.increment();
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), 1);
	}

	@Test
	public void testRate10HzNonBatch()
	throws Exception {
		final int rateLimit = 10;
		final int timeLimitSec = 10;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				while(true) {
					throttle.getPassFor(subj);
					counter.increment();
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), rateLimit * timeLimitSec / 10);
	}

	@Test
	public void testRate100kHzNonBatch()
	throws Exception {
		final int rateLimit = 100_000;
		final int timeLimitSec = 100;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				while(true) {
					throttle.getPassFor(subj);
					counter.increment();
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), rateLimit * timeLimitSec / 2);
	}

	@Test
	public void testRate1HzBatch()
	throws Exception {
		final int rateLimit = 1;
		final int timeLimitSec = 100;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				int n;
				while(true) {
					n = throttle.getPassFor(subj, 10);
					counter.add(n);
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), 1);
	}

	@Test
	public void testRate100HzBatch()
	throws Exception {
		final int rateLimit = 100;
		final int timeLimitSec = 10;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				int n;
				while(true) {
					n = throttle.getPassFor(subj, 100);
					counter.add(n);
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), rateLimit * timeLimitSec / 10);
	}

	@Test
	public void testRate1MHzBatch()
	throws Exception {
		final int rateLimit = 1_000_000;
		final int timeLimitSec = 100;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				int n;
				while(true) {
					n = throttle.getPassFor(subj, 100);
					counter.add(n);
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), rateLimit * timeLimitSec / 2);
	}
}