package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.common.collection.OptLockBuffer;
import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.common.concurrent.SvcTaskBase;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.DaemonBase;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.Credential;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
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
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

/**
 Created by kurila on 11.07.16.
 */
public abstract class StorageDriverBase<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements StorageDriver<I, O> {

	private static final Logger LOG = LogManager.getLogger();
	
	private final int batchSize;
	private final int queueCapacity;
	protected final BlockingQueue<O> childTasksQueue;
	private final BlockingQueue<O> inTasksQueue;
	private final BlockingQueue<O> ioResultsQueue;
	protected final String jobName;
	protected final int concurrencyLevel;
	protected final Semaphore concurrencyThrottle;
	protected final Credential credential;
	protected final boolean verifyFlag;
	private final LongAdder scheduledTaskCount = new LongAdder();
	private final LongAdder completedTaskCount = new LongAdder();
	
	private final ConcurrentMap<String, String> pathMap = new ConcurrentHashMap<>(1);
	protected abstract String requestNewPath(final String path);
	protected Function<String, String> requestPathFunc = this::requestNewPath;
	
	protected final ConcurrentMap<Credential, String> authTokens = new ConcurrentHashMap<>(1);
	protected abstract String requestNewAuthToken(final Credential credential);
	protected Function<Credential, String> requestAuthTokenFunc = this::requestNewAuthToken;
	private final IoTasksDispatch ioTasksDispatchTask;
	
	protected StorageDriverBase(
		final String jobName, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final boolean verifyFlag
	) throws UserShootHisFootException {
		this.batchSize = loadConfig.getBatchConfig().getSize();
		this.queueCapacity = loadConfig.getQueueConfig().getSize();
		this.childTasksQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.inTasksQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.ioResultsQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.jobName = jobName;
		final AuthConfig authConfig = storageConfig.getAuthConfig();
		this.credential = Credential.getInstance(authConfig.getUid(), authConfig.getSecret());
		final String authToken = authConfig.getToken();
		if(authToken != null) {
			if(this.credential == null) {
				this.authTokens.put(Credential.NONE, authToken);
			} else {
				this.authTokens.put(credential, authToken);
			}
		}
		this.concurrencyLevel = storageConfig.getDriverConfig().getConcurrency();
		this.concurrencyThrottle = new Semaphore(concurrencyLevel, true);
		this.verifyFlag = verifyFlag;
		this.ioTasksDispatchTask = new IoTasksDispatch(svcTasks);
		svcTasks.add(ioTasksDispatchTask);
	}

	private final class IoTasksDispatch
	extends SvcTaskBase {

		private final OptLockBuffer<O> buff = new OptLockArrayBuffer<>(batchSize);
		private int n = 0;
		private int m;

		public IoTasksDispatch(final List<SvcTask> svcTasks) {
			super(svcTasks);
		}

		@Override
		protected final void invoke() {
			if(buff.tryLock()) {
				try {
					if(n < batchSize) {
						n += childTasksQueue.drainTo(buff, batchSize - n);
					}
					if(n < batchSize) {
						n += inTasksQueue.drainTo(buff, batchSize - n);
					}
					LockSupport.parkNanos(1);
					if(n > 0) {
						if(n == 1) {
							if(submit(buff.get(0))) {
								buff.clear();
								n --;
							}
						} else {
							m = submit(buff, 0, n);
							if(m > 0) {
								buff.removeRange(0, m);
								n -= m;
							}
						}
					}
				} catch(final InterruptedException ignored) {
				} finally {
					buff.unlock();
				}
			}
		}

		@Override
		protected final void doClose() {
			try {
				if(buff.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
					buff.clear();
				} else {
					LOG.warn(
						Markers.ERR, "{}: failed to obtain the I/O tasks buffer lock in time",
						StorageDriverBase.this.toString()
					);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "{}: interrupted on close",
					StorageDriverBase.this.toString()
				);
			}
		}
	}

	@Override
	public final boolean put(final O task)
	throws EOFException, ServerException {
		if(!isStarted()) {
			throw new EOFException();
		}
		checkStateFor(task);
		if(inTasksQueue.offer(task)) {
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
		ioTask.reset();
		if(requestAuthTokenFunc != null) {
			final Credential credential = ioTask.getCredential();
			if(credential != null) {
				authTokens.computeIfAbsent(credential, requestAuthTokenFunc);
			}
		}
		if(requestPathFunc != null) {
			final String dstPath = ioTask.getDstPath();
			if(dstPath != null) {
				pathMap.computeIfAbsent(dstPath, requestPathFunc);
			}
		}
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
	public final O get() {
		return ioResultsQueue.poll();
	}
	
	@Override
	public final List<O> getAll() {
		final List<O> ioTaskResults = new ArrayList<>(batchSize);
		ioResultsQueue.drainTo(ioTaskResults, queueCapacity);
		return ioTaskResults;
	}
	
	@Override
	public final long skip(final long count) {
		int n = (int) Math.min(count, Integer.MAX_VALUE);
		final List<O> tmpBuff = new ArrayList<>(n);
		n = ioResultsQueue.drainTo(tmpBuff, n);
		tmpBuff.clear();
		return n;
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
	protected void doShutdown() {
		svcTasks.remove(ioTasksDispatchTask);
		try {
			ioTasksDispatchTask.close();
		} catch(final IOException ignored) {
		}
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
		super.doClose();
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
