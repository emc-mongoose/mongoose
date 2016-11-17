package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.model.io.IoType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;
/**
 Created by andrey on 06.11.16.
 */

public class WeightThrottleTest {

	private final Object2IntMap<IoType> weightMap = new Object2IntOpenHashMap<IoType>() {
		{
			put(IoType.CREATE, 80);
			put(IoType.READ, 20);
		}
	};

	private final Map<IoType, LongAdder> resultsMap = new HashMap<IoType, LongAdder>() {
		{
			put(IoType.CREATE, new LongAdder());
			put(IoType.READ, new LongAdder());
		}
	};

	private final Throttle<IoType> fc = new WeightThrottle<>(weightMap);

	private final class SubmTask
		implements Runnable {
		private final IoType ioType;
		public SubmTask(final IoType ioType) {
			this.ioType = ioType;
		}
		@Override
		public final void run() {
			while(true) {
				try {
					if(fc.getPassFor(ioType)) {
						resultsMap.get(ioType).increment();
					}
				} catch(final InterruptedException e) {
					break;
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
		final double writes = resultsMap.get(IoType.CREATE).sum();
		final long reads = resultsMap.get(IoType.READ).sum();
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
				try {
					n = fc.getPassFor(ioType, 128);
					if(n > 0) {
						resultsMap.get(ioType).add(n);
					}
				} catch(final InterruptedException e) {
					break;
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
		final double writes = resultsMap.get(IoType.CREATE).sum();
		final long reads = resultsMap.get(IoType.READ).sum();
		assertEquals(80/20, writes / reads, 0.01);
		System.out.println("Write rate: " + writes / 10 + " Hz, read rate: " + reads / 10 + " Hz");
	}
}
