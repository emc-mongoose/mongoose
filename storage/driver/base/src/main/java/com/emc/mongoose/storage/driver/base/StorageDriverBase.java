package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.DaemonBase;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 Created by kurila on 11.07.16.
 */
public abstract class StorageDriverBase<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements StorageDriver<I, O> {

	private static final Logger LOG = LogManager.getLogger();
	
	private final int queueCapacity;
	protected final BlockingQueue<O> childTasksQueue;
	private final BlockingQueue<O> inTasksQueue;
	private final BlockingQueue<O> ioResultsQueue;
	protected final String jobName;
	protected final int concurrencyLevel;
	protected final Semaphore concurrencyThrottle;
	protected final String uid;
	protected final String secret;
	protected volatile String authToken;
	protected final boolean verifyFlag;
	private final LongAdder scheduledTaskCount = new LongAdder();
	private final LongAdder completedTaskCount = new LongAdder();
	
	private final ConcurrentMap<String, String> pathMap = new ConcurrentHashMap<>(1);
	private final Function<String, String> createPathFunc = path -> {
		if(createPath(path)) {
			return path;
		} else {
			return null;
		}
	};
	
	protected abstract boolean createPath(final String path);
	
	protected StorageDriverBase(
		final String jobName, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final boolean verifyFlag
	) throws UserShootHisFootException {
		this.queueCapacity = loadConfig.getQueueConfig().getSize();
		this.childTasksQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.inTasksQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.ioResultsQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.jobName = jobName;
		final AuthConfig authConfig = storageConfig.getAuthConfig();
		this.uid = authConfig.getUid();
		this.secret = authConfig.getSecret();
		this.authToken = authConfig.getToken();
		this.concurrencyLevel = storageConfig.getDriverConfig().getConcurrency();
		this.concurrencyThrottle = new Semaphore(concurrencyLevel, true);
		this.verifyFlag = verifyFlag;

		SVC_TASKS.put(this, new IoTasksDispatch());
	}

	private final class IoTasksDispatch
	extends ArrayList<O> // extends ArrayList in order to get the access to the "removeRange" method
	implements Runnable {

		private int n = 0;
		private int m;

		public IoTasksDispatch() {
			super(BATCH_SIZE);
		}

		@Override
		public final void run() {
			if(n < BATCH_SIZE) {
				n += childTasksQueue.drainTo(this, BATCH_SIZE - n);
			}
			if(n < BATCH_SIZE) {
				n += inTasksQueue.drainTo(this, BATCH_SIZE - n);
			}
			try {
				if(n > 0) {
					m = submit(this, 0, n);
					if(m > 0) {
						removeRange(0, m);
						n -= m;
					}
				}
			} catch(final InterruptedException e) {
				SVC_TASKS.clear();
			}
		}
	}

	@Override
	public final boolean put(final O task)
	throws EOFException, ServerException {
		if(!isStarted()) {
			throw new EOFException();
		}
		if(inTasksQueue.offer(task)) {
			checkStateFor(task);
			scheduledTaskCount.increment();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public final int put(final List<O> tasks, final int from, final int to)
	throws EOFException, ServerException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int i = from;
		O nextTask;
		while(i < to && isStarted()) {
			nextTask = tasks.get(i);
			checkStateFor(nextTask);
			if(inTasksQueue.offer(tasks.get(i))) {
				i ++;
			} else {
				break;
			}
		}
		final int n = i - from;
		scheduledTaskCount.add(n);
		return n;
	}

	@Override
	public final int put(final List<O> tasks)
	throws EOFException, ServerException {
		if(!isStarted()) {
			throw new EOFException();
		}
		int n = 0;
		for(final O nextIoTask : tasks) {
			if(isStarted()) {
				checkStateFor(nextIoTask);
				if(inTasksQueue.offer(nextIoTask)) {
					n ++;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		scheduledTaskCount.add(n);
		return n;
	}
	
	private void checkStateFor(final O ioTask)
	throws ServerException {
		pathMap.computeIfAbsent(ioTask.getDstPath(), createPathFunc);
	}
	
	@Override
	public final int getConcurrencyLevel() {
		return concurrencyLevel;
	}
	
	@Override
	public final int getActiveTaskCount() {
		return concurrencyLevel - concurrencyThrottle.availablePermits();
	}
	
	@Override
	public final long getScheduledTaskCount() {
		return scheduledTaskCount.sum();
	}
	
	@Override
	public final long getCompletedTaskCount() {
		return completedTaskCount.sum();
	}

	@Override
	public final boolean isIdle() {
		return !concurrencyThrottle.hasQueuedThreads() &&
			concurrencyThrottle.availablePermits() >= concurrencyLevel;
	}

	@Override
	public List<O> getResults()
	throws IOException {
		final List<O> ioTaskResults = new ArrayList<>(BATCH_SIZE);
		ioResultsQueue.drainTo(ioTaskResults, queueCapacity);
		return ioTaskResults;
	}

	@SuppressWarnings("unchecked")
	protected final void ioTaskCompleted(final O ioTask) {

		completedTaskCount.increment();

		try {

			final O ioTaskResult = ioTask.getResult();
			if(!ioResultsQueue.offer(ioTaskResult, 1, TimeUnit.MILLISECONDS)) {
				LOG.warn(
					Markers.ERR, "{}: I/O task results queue overflow, dropping the result",
					toString()
				);
			}

			if(ioTask instanceof CompositeIoTask) {
				final CompositeIoTask parentTask = (CompositeIoTask) ioTask;
				if(!parentTask.allSubTasksDone()) {
					final List<O> subTasks = parentTask.getSubTasks();
					for(final O nextSubTask : subTasks) {
						if(!childTasksQueue.offer(nextSubTask, 1, TimeUnit.MILLISECONDS)) {
							LOG.warn(
								Markers.ERR,
								"{}: I/O child tasks queue overflow, dropping the I/O sub-task",
								toString()
							);
							break;
						}
					}
				}
			} else if(ioTask instanceof PartialIoTask) {
				final PartialIoTask subTask = (PartialIoTask) ioTask;
				final CompositeIoTask parentTask = subTask.getParent();
				if(parentTask.allSubTasksDone()) {
					// execute once again to finalize the things if necessary:
					// complete the multipart upload, for example
					if(!childTasksQueue.offer((O) parentTask, 1, TimeUnit.MILLISECONDS)) {
						LOG.warn(
							Markers.ERR,
							"{}: I/O child tasks queue overflow, dropping the I/O task",
							toString()
						);
					}
				}
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted the completed I/O task processing");
		}
	}
	
	/*protected final int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to) {

		int i;

		if(isCircular) {
			try {
				for(i = from; i < to; i += inTasksQueue.put(ioTasks, i, to)) {
					LockSupport.parkNanos(1);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to enqueue {} I/O tasks for the next execution",
					to - from
				);
			}
		}

		try {
			for(i = from; i < to; i += outTasksQueue.put(ioTasks, i, to)) {
				LockSupport.parkNanos(1);
			}
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to put {} I/O tasks to the output buffer", to - from
			);
		}

		return to - from;
	}*/

	protected abstract boolean submit(final O ioTask)
	throws InterruptedException;

	protected abstract int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException;

	protected abstract int submit(final List<O> ioTasks)
	throws InterruptedException;

	@Override
	public Input<O> getInput() {
		return null;
	}
	
	@Override
	public String getAuthToken()
	throws RemoteException {
		return authToken;
	}
	
	@Override
	public void setAuthToken(final String authToken) {
		this.authToken = authToken;
	}

	@Override
	protected void doShutdown() {
		SVC_TASKS.remove(this);
		LOG.debug(Markers.MSG, "{}: shut down", toString());
	}

	@Override
	protected void doInterrupt() {
		try {
			if(!concurrencyThrottle.tryAcquire(concurrencyLevel, 10, TimeUnit.MILLISECONDS)) {
				LOG.debug(Markers.MSG, "{}: interrupting while not in the idle state", toString());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to await the idle state");
		} finally {
			LOG.debug(Markers.MSG, "{}: interrupted", toString());
		}
	}

	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		childTasksQueue.clear();
		inTasksQueue.clear();
		final int ioResultsQueueSize = ioResultsQueue.size();
		if(ioResultsQueueSize > 0) {
			LOG.warn(
				Markers.ERR, "{}: I/O results queue contains {} unhandled elements", toString(),
				ioResultsQueueSize
			);
		}
		ioResultsQueue.clear();
		pathMap.clear();
		LOG.debug(Markers.MSG, "{}: closed", toString());
	}
	
	@Override
	public String toString() {
		return "storage/driver/" + concurrencyLevel + "/%s/" + hashCode();
	}
}
