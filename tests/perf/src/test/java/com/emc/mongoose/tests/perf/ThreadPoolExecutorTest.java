package com.emc.mongoose.tests.perf;

import com.emc.mongoose.api.common.concurrent.FutureTaskBase;
import com.emc.mongoose.api.common.concurrent.ThreadUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 30.03.17.
 */
@Ignore
public class ThreadPoolExecutorTest {
	
	private static final int BATCH_SIZE = 0x1000;
	private static final int QUEUE_SIZE_LIMIT = 1_000_000;
	private static final int TIME_LIMIT_SEC = 50;
	
	private static final class UnpooledCounterIncrementTask<Void>
	extends FutureTaskBase<Void> {
		
		private final LongAdder sharedCounter;
		
		public UnpooledCounterIncrementTask(final LongAdder sharedCounter) {
			this.sharedCounter = sharedCounter;
		}
		
		@Override
		public final void run() {
			sharedCounter.increment();
		}
	}
	
	@Test
	public final void testUnpooledTasksRate()
	throws Exception {
		final ExecutorService executor = new ThreadPoolExecutor(
			ThreadUtil.getHardwareThreadCount(),
			ThreadUtil.getHardwareThreadCount(),
			0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_SIZE_LIMIT)
		);
		
		final LongAdder completedTaskCounter = new LongAdder();
		
		final Thread t = new Thread(() -> {
			RunnableFuture<Void> task;
			while(true) {
				task = new UnpooledCounterIncrementTask<>(completedTaskCounter);
				while(true) {
					try {
						executor.execute(task);
						break;
					} catch(final RejectedExecutionException ignored) {
						try {
							Thread.sleep(1);
						} catch(final InterruptedException e) {
							break;
						}
					}
				}
			}
		});
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		System.out.println("Unpooled tasks rate: " + completedTaskCounter.sum() / TIME_LIMIT_SEC);
		t.interrupt();
		executor.shutdownNow();
	}
	
	private static final class PooledCounterIncrementTask<Void>
	extends FutureTaskBase<Void> {
		
		private LongAdder sharedCounter;
		
		private PooledCounterIncrementTask() {
		}
		
		private static final Deque<PooledCounterIncrementTask> POOL = new ConcurrentLinkedDeque<>();
		
		public static PooledCounterIncrementTask getInstance(final LongAdder sharedCounter) {
			PooledCounterIncrementTask task = POOL.poll();
			if(task == null) {
				task = new PooledCounterIncrementTask();
			}
			task.sharedCounter = sharedCounter;
			return task;
		}
		
		@Override
		public final void run() {
			sharedCounter.increment();
			POOL.offer(this);
		}
	}
	
	@Test
	public final void testPooledTasksRate()
	throws Exception {
		final ExecutorService executor = new ThreadPoolExecutor(
			ThreadUtil.getHardwareThreadCount(),
			ThreadUtil.getHardwareThreadCount(),
			0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_SIZE_LIMIT)
		);
		
		final LongAdder completedTaskCounter = new LongAdder();
		
		final Thread t = new Thread(() -> {
			RunnableFuture<Void> task;
			while(true) {
				task = PooledCounterIncrementTask.getInstance(completedTaskCounter);
				while(true) {
					try {
						executor.execute(task);
						break;
					} catch(final RejectedExecutionException ignored) {
						try {
							Thread.sleep(1);
						} catch(final InterruptedException e) {
							break;
						}
					}
				}
			}
		});
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		System.out.println("Pooled tasks rate: " + completedTaskCounter.sum() / TIME_LIMIT_SEC);
		t.interrupt();
		executor.shutdownNow();
	}
	
	@Test
	public final void testPooledTasksRate2()
	throws Exception {
		final ExecutorService executor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount()
		);
		
		final BlockingQueue<RunnableFuture> tasksQueue = new ArrayBlockingQueue<>(QUEUE_SIZE_LIMIT);
		for(int i = 0; i < ThreadUtil.getHardwareThreadCount(); i ++) {
			executor.submit(
				() -> {
					final List<RunnableFuture> tasksBuff = new ArrayList<>(BATCH_SIZE);
					final Thread currentThread = Thread.currentThread();
					while(!currentThread.isInterrupted()) {
						tasksQueue.drainTo(tasksBuff, BATCH_SIZE);
						for(final RunnableFuture nextTask : tasksBuff) {
							nextTask.run();
						}
						tasksBuff.clear();
					}
				}
			);
		}
		
		final LongAdder completedTaskCounter = new LongAdder();
		
		final Thread t = new Thread(() -> {
			RunnableFuture<Void> task;
			while(true) {
				task = PooledCounterIncrementTask.getInstance(completedTaskCounter);
				try {
					tasksQueue.put(task);
				} catch(final InterruptedException e) {
					break;
				}
			}
		});
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		System.out.println("Pooled tasks rate 2: " + completedTaskCounter.sum() / TIME_LIMIT_SEC);
		t.interrupt();
		executor.shutdownNow();
	}
	
	@Test
	public final void testUnpooledTasksRate2()
	throws Exception {
		final ExecutorService executor = Executors.newFixedThreadPool(
			ThreadUtil.getHardwareThreadCount()
		);
		
		final BlockingQueue<RunnableFuture> tasksQueue = new ArrayBlockingQueue<>(QUEUE_SIZE_LIMIT);
		for(int i = 0; i < ThreadUtil.getHardwareThreadCount(); i ++) {
			executor.submit(
				() -> {
					final List<RunnableFuture> tasksBuff = new ArrayList<>(BATCH_SIZE);
					final Thread currentThread = Thread.currentThread();
					while(!currentThread.isInterrupted()) {
						tasksQueue.drainTo(tasksBuff, BATCH_SIZE);
						for(final RunnableFuture nextTask : tasksBuff) {
							nextTask.run();
						}
						tasksBuff.clear();
					}
				}
			);
		}
		
		final LongAdder completedTaskCounter = new LongAdder();
		
		final Thread t = new Thread(() -> {
			RunnableFuture<Void> task;
			while(true) {
				task = new UnpooledCounterIncrementTask(completedTaskCounter);
				try {
					tasksQueue.put(task);
				} catch(final InterruptedException e) {
					break;
				}
			}
		});
		t.start();
		TimeUnit.SECONDS.timedJoin(t, TIME_LIMIT_SEC);
		System.out.println("Unpooled tasks rate 2: " + completedTaskCounter.sum() / TIME_LIMIT_SEC);
		t.interrupt();
		executor.shutdownNow();
	}
}
