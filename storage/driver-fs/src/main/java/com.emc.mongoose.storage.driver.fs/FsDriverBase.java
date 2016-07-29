package com.emc.mongoose.storage.driver.fs;

import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.util.IoWorker;
import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.storage.driver.base.DriverBase;
import static com.emc.mongoose.ui.config.Config.IoConfig.BufferConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 19.07.16.
 */
public abstract class FsDriverBase<I extends Item, O extends IoTask<I>>
extends DriverBase<I, O>
implements Driver<I, O> {

	private final static Logger LOG = LogManager.getLogger();
	private final static int BATCH_SIZE = 0x80;

	private final ThreadPoolExecutor ioTaskExecutor;
	private final int ioWorkerCount;
	private final BlockingQueue<O> ioTaskQueue;

	public FsDriverBase(
		final LoadConfig loadConfig, final BufferConfig ioBufferConfig
	) {
		super(loadConfig);
		final SizeInBytes ioBuffSize = ioBufferConfig.getSize();
		final long ioBuffSizeMin = ioBuffSize.getMin();
		final long ioBuffSizeMax = ioBuffSize.getMax();
		if(ioBuffSizeMin < 1 || ioBuffSizeMin > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Invalid I/O buff size min: " + ioBuffSizeMin);
		}
		if(ioBuffSizeMax < ioBuffSizeMin || ioBuffSizeMax > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Invalid I/O buff size max: " + ioBuffSizeMax);
		}
		ioTaskQueue = new ArrayBlockingQueue<>(loadConfig.getQueueConfig().getSize());
		ioWorkerCount = ThreadUtil.getAvailableConcurrencyLevel();
		ioTaskExecutor = new ThreadPoolExecutor(ioWorkerCount, ioWorkerCount, 0, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(ioWorkerCount),
			new IoWorker.Factory("ioWorker", (int) ioBuffSize.getMin(), (int) ioBuffSize.getMax())
		);
	}

	public final class IoTaskSchedule
	implements Runnable {

		private final List<O> ioTaskBuff = new ArrayList<>(BATCH_SIZE);

		@Override
		public final void run() {
			final Thread currentThread = Thread.currentThread();
			IoTask.Status nextStatus;
			try {
				while(!FsDriverBase.this.isInterrupted() && !currentThread.isInterrupted()) {
					ioTaskBuff.clear();
					ioTaskQueue.drainTo(ioTaskBuff, BATCH_SIZE);
					for(final O ioTask : ioTaskBuff) {
						executeIoTask(ioTask);
						nextStatus = ioTask.getStatus();
						if(nextStatus.equals(IoTask.Status.ACTIVE)) {
							ioTaskQueue.put(ioTask);
						} else {
							monitorRef.get().ioTaskCompleted(ioTask);
						}
					}
				}
			} catch(final InterruptedException ignored) {
			}
		}
	}

	protected abstract void executeIoTask(final O ioTask);
	
	@Override
	protected void doStart()
	throws UserShootHisFootException {
		for(int i = 0; i < ioWorkerCount; i ++) {
			ioTaskExecutor.submit(new IoTaskSchedule());
		}
	}

	@Override
	protected void doShutdown()
	throws UserShootHisFootException {
		ioTaskExecutor.shutdown();
	}

	@Override
	protected void doInterrupt()
	throws UserShootHisFootException {
		final List<Runnable> interruptedTasks = ioTaskExecutor.shutdownNow();
		LOG.debug(Markers.MSG, "{} I/O tasks dropped", interruptedTasks.size());
	}

	@Override
	public boolean isFullThrottleEntered() {
		return false;
	}

	@Override
	public boolean isFullThrottleExited() {
		return false;
	}

	@Override
	public void submit(final O ioTask)
	throws InterruptedException {
		ioTaskQueue.put(ioTask);
	}

	@Override
	public int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException {
		for(int i = from; i < to; i ++) {
			ioTaskQueue.put(ioTasks.get(i));
		}
		return to - from;
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return await(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return ioTaskExecutor.awaitTermination(timeout, timeUnit);
	}

	@Override
	public void close()
	throws IOException {
		super.close();
		ioTaskQueue.clear();
	}
}
