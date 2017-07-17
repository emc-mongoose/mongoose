package com.emc.mongoose.tests.perf;

import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.common.io.Output;
import com.emc.mongoose.api.model.svc.RoundRobinOutputsTransferSvcTask;

import com.emc.mongoose.api.model.DaemonBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.fail;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
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
public class RoundRobinOutputsTransferSvcTaskTest {
	
	private static final int BATCH_SIZE = 0x1000;
	private static final int TEST_TIME_LIMIT_SEC = 30;

	private final static class DaemonMock
	extends DaemonBase {

		@Override
		public boolean await(final long timeout, final TimeUnit timeUnit)
		throws InterruptedException, RemoteException {
			return false;
		}

		@Override
		protected void doShutdown()
		throws IllegalStateException {
		}

		@Override
		protected void doInterrupt()
		throws IllegalStateException {
		}
	}
	
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
	
	public RoundRobinOutputsTransferSvcTaskTest(final int outputCount)
	throws Exception {
		this.outputCount = outputCount;
		outputs = new ArrayList<>(outputCount);
		for(int i = 0; i < outputCount; i ++) {
			outputs.add(new CountingOutput());
		}
		try(final DaemonMock daemonMock = new DaemonMock()) {
			rrcOutput = new RoundRobinOutputsTransferSvcTask(outputs, daemonMock.getSvcTasks(), BATCH_SIZE);
			final Thread t = new Thread(() -> {
				final Thread currentThread = Thread.currentThread();
				final List buff = new ArrayList(BATCH_SIZE);
				for(int i = 0; i < BATCH_SIZE; i ++) {
					buff.add(new Object());
				}
				try {
					int i;
					while(!currentThread.isInterrupted()) {
						i = 0;
						while(i < BATCH_SIZE) {
							i += rrcOutput.put(buff, i, BATCH_SIZE);
						}
					}
				} catch(final EOFException ignored) {
				} catch(final Throwable e) {
					fail(e.toString());
				}
			});
			daemonMock.start();
			t.start();
			TimeUnit.SECONDS.timedJoin(t, TEST_TIME_LIMIT_SEC);
			t.interrupt();
		}
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
