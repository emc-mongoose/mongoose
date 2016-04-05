package com.emc.mongoose.core.impl.load.model;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.barrier.Barrier;
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
import com.emc.mongoose.core.impl.load.barrier.WeightBarrier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
/**
 Created by kurila on 29.03.16.
 */
public class WeightBarrierTest {

	private final Map<LoadType, Integer> weightMap = new HashMap<LoadType, Integer>() {
		{
			put(LoadType.WRITE, 80);
			put(LoadType.READ, 20);
		}
	};

	private final Map<LoadType, AtomicInteger> resultsMap = new HashMap<LoadType, AtomicInteger>() {
		{
			put(LoadType.WRITE, new AtomicInteger(0));
			put(LoadType.READ, new AtomicInteger(0));
		}
	};

	private final Barrier<IOTask> fc = new WeightBarrier<>(weightMap);

	private final class IOTaskMock
	implements IOTask {
		public LoadType loadType = null;
		@Override
		public String getNodeAddr() {
			return null;
		}
		@Override
		public Item getItem() {
			return null;
		}
		@Override
		public Status getStatus() {
			return null;
		}
		@Override
		public void mark(final IOStats ioStats) {
		}
		@Override
		public Object getKey() {
			return loadType;
		}
		@Override
		public Object getValue() {
			return null;
		}
		@Override
		public Object setValue(final Object value) {
			return null;
		}
	}

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
					final IOTaskMock ioTask = new IOTaskMock();
					ioTask.loadType = loadType;
					if(fc.getApprovalFor(ioTask)) {
						resultsMap.get(loadType).incrementAndGet();
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
		es.submit(new SubmTask(LoadType.WRITE));
		es.submit(new SubmTask(LoadType.READ));
		es.awaitTermination(100, TimeUnit.SECONDS);
		es.shutdownNow();
		final double writes = resultsMap.get(LoadType.WRITE).get();
		final long reads = resultsMap.get(LoadType.READ).get();
		assertEquals(80/20, writes / reads, 0.01);
		System.out.println("Rate was: " + (writes + reads) / 10 + " per sec");
	}

	private final class BatchSubmTask
	implements Runnable {
		private final LoadType loadType;
		public BatchSubmTask(final LoadType loadType) {
			this.loadType = loadType;
		}
		@Override
		public final void run() {
			while(true) {
				try {
					final List<IOTask> ioTasks = new ArrayList<>();
					IOTaskMock ioTask;
					for(int i = 0; i < 128; i ++) {
						ioTask = new IOTaskMock();
						ioTask.loadType = loadType;
						ioTasks.add(ioTask);
					}
					if(fc.getApprovalsFor(ioTasks, 0, 128)) {
						resultsMap.get(loadType).incrementAndGet();
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
		es.submit(new BatchSubmTask(LoadType.WRITE));
		es.submit(new BatchSubmTask(LoadType.READ));
		es.awaitTermination(100, TimeUnit.SECONDS);
		es.shutdownNow();
		final double writes = resultsMap.get(LoadType.WRITE).get();
		final long reads = resultsMap.get(LoadType.READ).get();
		assertEquals(80/20, writes / reads, 0.01);
		System.out.println("Rate was: " + 128 * (writes + reads) / 10 + " per sec");
	}
}
