package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
/**
 Created by andrey on 06.11.16.
 */

public class WeightThrottleTest {

	private final Object2IntMap<LoadType> weightMap = new Object2IntOpenHashMap<LoadType>() {
		{
			put(LoadType.CREATE, 80);
			put(LoadType.READ, 20);
		}
	};

	private final Map<LoadType, LongAdder> resultsMap = new HashMap<LoadType, LongAdder>() {
		{
			put(LoadType.CREATE, new LongAdder());
			put(LoadType.READ, new LongAdder());
		}
	};

	private final Throttle<LoadType> fc = new WeightThrottle<>(weightMap);

	private final class SubmTask
		implements Runnable {
		private final LoadType loadType;
		public SubmTask(final LoadType loadType) {
			this.loadType = loadType;
		}
		@Override
		public final void run() {
			while(true) {
				try {
					if(fc.getPassFor(loadType)) {
						resultsMap.get(loadType).increment();
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
		es.submit(new SubmTask(LoadType.CREATE));
		es.submit(new SubmTask(LoadType.READ));
		es.awaitTermination(10, TimeUnit.SECONDS);
		es.shutdownNow();
		final double writes = resultsMap.get(LoadType.CREATE).sum();
		final long reads = resultsMap.get(LoadType.READ).sum();
		assertEquals(80/20, writes / reads, 0.01);
		System.out.println("Write rate: " + writes / 10 + " Hz, read rate: " + reads / 10 + " Hz");
	}

	private final class BatchSubmTask
		implements Runnable {
		private final LoadType loadType;
		public BatchSubmTask(final LoadType loadType) {
			this.loadType = loadType;
		}
		@Override
		public final void run() {
			int n;
			while(true) {
				try {
					n = fc.getPassFor(loadType, 128);
					if(n > 0) {
						resultsMap.get(loadType).add(n);
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
		es.submit(new BatchSubmTask(LoadType.CREATE));
		es.submit(new BatchSubmTask(LoadType.READ));
		es.awaitTermination(10, TimeUnit.SECONDS);
		es.shutdownNow();
		final double writes = resultsMap.get(LoadType.CREATE).sum();
		final long reads = resultsMap.get(LoadType.READ).sum();
		assertEquals(80/20, writes / reads, 0.01);
		System.out.println("Write rate: " + writes / 10 + " Hz, read rate: " + reads / 10 + " Hz");
	}
}
