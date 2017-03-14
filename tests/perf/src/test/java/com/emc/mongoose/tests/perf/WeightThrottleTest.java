package com.emc.mongoose.tests.perf;

import com.emc.mongoose.common.concurrent.WeightThrottle;
import com.emc.mongoose.model.io.IoType;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertEquals;
/**
 Created by andrey on 06.11.16.
 */

public class WeightThrottleTest {

	private final Int2IntMap weightMap = new Int2IntOpenHashMap() {
		{
			put(IoType.CREATE.ordinal(), 80);
			put(IoType.READ.ordinal(), 20);
		}
	};

	private final Int2ObjectMap<LongAdder> resultsMap = new Int2ObjectOpenHashMap<LongAdder>() {
		{
			put(IoType.CREATE.ordinal(), new LongAdder());
			put(IoType.READ.ordinal(), new LongAdder());
		}
	};

	private final WeightThrottle wt = new WeightThrottle(weightMap);

	private final class SubmTask
		implements Runnable {
		private final IoType ioType;
		public SubmTask(final IoType ioType) {
			this.ioType = ioType;
		}
		@Override
		public final void run() {
			while(true) {
				if(wt.tryAcquire(ioType.ordinal())) {
					resultsMap.get(ioType.ordinal()).increment();
				} else {
					LockSupport.parkNanos(1);
				}
			}
		}
	}

	@Test
	public void testRequestApprovalFor()
	throws Exception {
		final ExecutorService es = Executors.newFixedThreadPool(2);
		es.submit(new SubmTask(IoType.CREATE));
		es.submit(new SubmTask(IoType.READ));
		es.awaitTermination(10, TimeUnit.SECONDS);
		es.shutdownNow();
		final double writes = resultsMap.get(IoType.CREATE.ordinal()).sum();
		final long reads = resultsMap.get(IoType.READ.ordinal()).sum();
		assertEquals(80/20, writes / reads, 0.01);
		System.out.println("Write rate: " + writes / 10 + " Hz, read rate: " + reads / 10 + " Hz");
	}

	private final class BatchSubmTask
	implements Runnable {
		private final IoType ioType;
		public BatchSubmTask(final IoType ioType) {
			this.ioType = ioType;
		}
		@Override
		public final void run() {
			int n;
			while(true) {
				n = wt.tryAcquire(ioType.ordinal(), 128);
				if(n > 0) {
					resultsMap.get(ioType.ordinal()).add(n);
				} else {
					LockSupport.parkNanos(1);
				}
			}
		}
	}

	@Test
	public void testRequestBatchApprovalFor()
	throws Exception {
		final ExecutorService es = Executors.newFixedThreadPool(2);
		es.submit(new BatchSubmTask(IoType.CREATE));
		es.submit(new BatchSubmTask(IoType.READ));
		es.awaitTermination(10, TimeUnit.SECONDS);
		es.shutdownNow();
		final double writes = resultsMap.get(IoType.CREATE.ordinal()).sum();
		final long reads = resultsMap.get(IoType.READ.ordinal()).sum();
		assertEquals(80/20, writes / reads, 0.01);
		System.out.println("Write rate: " + writes / 10 + " Hz, read rate: " + reads / 10 + " Hz");
	}
}
