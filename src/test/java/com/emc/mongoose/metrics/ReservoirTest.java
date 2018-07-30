package com.emc.mongoose.metrics;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.UniformReservoir;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class ReservoirTest {

	private final int TIME_LIMIT = 100;

	@Test
	public final void testUniformReservoir()
	throws Exception {
		final UniformReservoir reservoir = new UniformReservoir();
		testReservoir(reservoir);
	}

	@Test
	public final void testExponentiallyDecayingReservoir()
	throws Exception {
		final ExponentiallyDecayingReservoir reservoir = new ExponentiallyDecayingReservoir();
		testReservoir(reservoir);
	}

	@Test
	public final void testConcurrentSlidingWindowReservoir()
	throws Exception {
		final ConcurrentSlidingWindowReservoir reservoir = new ConcurrentSlidingWindowReservoir();
		testReservoir(reservoir);
	}

	private void testReservoir(final Reservoir reservoir) {
		try {
			final FutureTask<Long> task = new FutureTask(new CallableAdder(reservoir));
			final Thread adder = new Thread(task);
			final Thread getter = new ThreadGetter(reservoir);
			adder.start();
			getter.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			adder.interrupt();
			getter.interrupt();
			final long count = task.get();
			System.out.println(reservoir.getClass().getName() + " : " + count / TIME_LIMIT);
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
}

class CallableAdder
	implements Callable {

	private final Reservoir reservoir;
	private final LongAdder counter;

	public CallableAdder(final Reservoir r) {
		reservoir = r;
		counter = new LongAdder();
	}

	@Override
	public Object call()
	throws Exception {
		while(! Thread.interrupted()) {
			reservoir.update(System.currentTimeMillis());
			counter.increment();
		}
		return counter.sum();
	}
}

class ThreadGetter
	extends Thread {

	private final Reservoir reservoir;

	public ThreadGetter(final Reservoir r) {
		reservoir = r;
	}

	@Override
	public void run() {
		while(! Thread.interrupted()) {
			reservoir.getSnapshot();
		}
	}
}
