package com.emc.mongoose.tests.perf;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.io.collection.AsyncRoundRobinOutput;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 29.03.17.
 */
@RunWith(Parameterized.class)
public class AsyncRoundRobinOutputTest {
	
	private static final int QUEUE_SIZE_LIMIT = 1_000_000;
	private static final int TEST_TIME_LIMIT_SEC = 30;
	
	private final static class CountingOutput<T>
	implements Output<T> {
		
		public final LongAdder count = new LongAdder();
		
		@Override
		public boolean put(final T item)
		throws IOException {
			count.increment();
			return true;
		}
		
		@Override
		public int put(final List<T> buffer, final int from, final int to)
		throws IOException {
			count.add(to - from);
			return to - from;
		}
		
		@Override
		public int put(final List<T> buffer)
		throws IOException {
			count.add(buffer.size());
			return buffer.size();
		}
		
		@Override
		public Input<T> getInput()
		throws IOException {
			return null;
		}
		
		@Override
		public void close()
		throws IOException {
		}
	}
	private final List<CountingOutput> outputs;
	private final Output rrcOutput;
	private final int outputCount;
	
	public AsyncRoundRobinOutputTest(final int outputCount)
	throws Exception {
		this.outputCount = outputCount;
		outputs = new ArrayList<>(outputCount);
		for(int i = 0; i < outputCount; i ++) {
			outputs.add(new CountingOutput());
		}
		rrcOutput = new AsyncRoundRobinOutput(outputs, QUEUE_SIZE_LIMIT);
		final Thread t = new Thread(() -> {
			final Thread currentThread = Thread.currentThread();
			final List buff = new ArrayList(BATCH_SIZE);
			for(int i = 0; i < BATCH_SIZE; i ++) {
				buff.add(new Object());
			}
			try {
				while(!currentThread.isInterrupted()) {
					if(BATCH_SIZE != rrcOutput.put(buff, 0, BATCH_SIZE)) {
						fail();
					}
				}
			} catch(final Throwable e) {
				fail(e.toString());
			}
		});
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TEST_TIME_LIMIT_SEC);
		t.interrupt();
	}
	
	@Parameterized.Parameters
	public static Collection<Object[]> generateData() {
		return Arrays.asList(
			new Object[][] {
				{1}, {2}, {5}, {10}, {20}, {50}, {100}
			}
		);
	}
	
	@Test
	public void test()
	throws Exception {
		long count = 0;
		for(final CountingOutput co : outputs) {
			count += co.count.sum();
		}
		System.out.println(
			"Rate for " + outputs.size() + " outputs: " + count / TEST_TIME_LIMIT_SEC
		);
	}
}
