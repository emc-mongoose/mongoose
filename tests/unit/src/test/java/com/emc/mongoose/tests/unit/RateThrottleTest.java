package com.emc.mongoose.tests.unit;

import com.emc.mongoose.load.monitor.RateThrottle;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

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
					if(throttle.getPassFor(subj)) {
						counter.increment();
					} else {
						LockSupport.parkNanos(1);
					}
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
					if(throttle.getPassFor(subj)) {
						counter.increment();
					} else {
						LockSupport.parkNanos(1);
					}
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), rateLimit * timeLimitSec / 100);
	}

	@Test
	public void testRate100kHzNonBatch()
	throws Exception {
		final int rateLimit = 100_000;
		final int timeLimitSec = 20;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				while(true) {
					if(throttle.getPassFor(subj)) {
						counter.increment();
					} else {
						LockSupport.parkNanos(1);
					}
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), rateLimit * timeLimitSec / 5);
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
					if(n > 0) {
						counter.add(n);
					} else {
						LockSupport.parkNanos(1);
					}
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), 10);
	}

	@Test
	public void testRate100HzBatch()
	throws Exception {
		final int rateLimit = 100;
		final int timeLimitSec = 100;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				int n;
				while(true) {
					n = throttle.getPassFor(subj, 100);
					if(n > 0) {
						counter.add(n);
					} else {
						LockSupport.parkNanos(1);
					}
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
		final int timeLimitSec = 10;
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final Object subj = new Object();
		final LongAdder counter = new LongAdder();
		final Thread submThread = new Thread(
			() -> {
				int n;
				while(true) {
					n = throttle.getPassFor(subj, 100);
					if(n > 0) {
						counter.add(n);
					} else {
						LockSupport.parkNanos(1);
					}
				}
			}
		);
		submThread.start();
		TimeUnit.SECONDS.timedJoin(submThread, timeLimitSec);
		submThread.interrupt();

		assertEquals(rateLimit * timeLimitSec, counter.sum(), rateLimit * timeLimitSec / 5);
	}

	@Test
	public void testRate1kHzBatchConcurrent() {
		final int rateLimit = 1_000;
		final int timeLimitSec = 20;
		final Object subj = new Object();
		final RateThrottle throttle = new RateThrottle(rateLimit);
		final LongAdder counter = new LongAdder();
		final ExecutorService execSvc = Executors.newFixedThreadPool(4);
		for(int i = 0; i < 4; i ++) {
			final int j = i;
			execSvc.submit(
				() -> {
					int n;
					while(true) {
						if(j == 0) {
							if(throttle.getPassFor(subj)) {
								counter.increment();
							} else {
								LockSupport.parkNanos(1);
							}
						} else {
							n = throttle.getPassFor(subj, 1 + j);
							if(n > 0) {
								counter.add(n);
							} else {
								LockSupport.parkNanos(1);
							}
						}
					}
				}
			);
		}
		execSvc.shutdown();
		try {
			execSvc.awaitTermination(timeLimitSec, TimeUnit.SECONDS);
			execSvc.shutdownNow();
			assertEquals(rateLimit * timeLimitSec, counter.sum(), rateLimit * timeLimitSec / 5);

		} catch(final InterruptedException ignored) {
		}
	}
}