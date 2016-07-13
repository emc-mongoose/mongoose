package com.emc.mongoose.common.concurrent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static com.emc.mongoose.common.concurrent.ConcurrentQueueTaskSequencer.INSTANCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 Created by kurila on 13.07.16.
 */
@RunWith(Parameterized.class)
public class ConcurrentQueueTaskSequencerPerfTest {

	private final static class RunnableFutureMock<V>
		implements RunnableFuture<V> {
		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public V get()
		throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public V get(final long timeout, final TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}

		@Override
		public void run() {
		}
	}

	private final static class SubmitTask
		implements Runnable {

		public SubmitTask(final int threadTaskCount) {
			this.threadTaskCount = threadTaskCount;
		}

		private final int threadTaskCount;
		@Override

		public final void run() {
			for(int j = 0; j < threadTaskCount; j ++) {
				INSTANCE.submit(new RunnableFutureMock<>());
			}
		}
	}

	@Parameters
	public static Iterable<Integer> data() {
		return Arrays.asList(1, 2, 3, 4, 5, 8, 10, 12, 16, 20, 25, 32, 48, 64, 80, 100, 128);
	}

	@Parameter
	public int threadCount;

	@Test
	public void testPerformance()
	throws InterruptedException {
		final int totalTaskCount = 10_000_000;
		final List<Thread> threads = new ArrayList<>();
		final int threadTaskCount = totalTaskCount / threadCount;
		for(int i = 0; i < threadCount; i ++) {
			threads.add(new Thread(new SubmitTask(threadTaskCount)));
		}
		for(final Thread thread : threads) {
			thread.setDaemon(true);
			thread.start();
		}
		long t = System.nanoTime();
		for(final Thread thread : threads) {
			thread.join();
		}
		t = System.nanoTime() - t;
		System.out.println(threadCount + ", " + (1e9 * totalTaskCount) / t);
	}
}
