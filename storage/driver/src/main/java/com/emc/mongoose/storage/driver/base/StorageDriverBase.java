package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.api.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.api.common.collection.OptLockBuffer;
import com.emc.mongoose.api.common.concurrent.SvcTask;
import com.emc.mongoose.api.common.concurrent.SvcTaskBase;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.model.DaemonBase;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.model.data.ContentSource;
import com.emc.mongoose.api.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.auth.AuthConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Collections;
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

	private final static String CLS_NAME = StorageDriverBase.class.getSimpleName();

	private final ContentSource contentSrc;
	private final int batchSize;
	private final int queueCapacity;
	protected final BlockingQueue<O> childTasksQueue;
	private final BlockingQueue<O> inTasksQueue;
	private final BlockingQueue<O> ioResultsQueue;
	protected final String stepName;
	protected final int concurrencyLevel;
	protected final Semaphore concurrencyThrottle;
	protected final Credential credential;
	protected final boolean verifyFlag;
	private final LongAdder scheduledTaskCount = new LongAdder();
	private final LongAdder completedTaskCount = new LongAdder();
	
	private final ConcurrentMap<String, String> pathMap = new ConcurrentHashMap<>(1);
	protected abstract String requestNewPath(final String path);
	protected Function<String, String> requestNewPathFunc = this::requestNewPath;
	
	protected final ConcurrentMap<Credential, String> authTokens = new ConcurrentHashMap<>(1);
	protected abstract String requestNewAuthToken(final Credential credential);
	protected Function<Credential, String> requestAuthTokenFunc = this::requestNewAuthToken;
	private final IoTasksDispatch ioTasksDispatchTask;
	
	protected StorageDriverBase(
		final String stepName, final ContentSource contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException {
		this.contentSrc = contentSrc;
		this.batchSize = loadConfig.getBatchConfig().getSize();
		this.queueCapacity = loadConfig.getQueueConfig().getSize();
		this.childTasksQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.inTasksQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.ioResultsQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.stepName = stepName;
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
				try(
					final Instance logCtx = CloseableThreadContext
						.put(KEY_TEST_STEP_ID, stepName)
						.put(KEY_CLASS_NAME, CLS_NAME)
				) {
					if(n < batchSize) {
						n += childTasksQueue.drainTo(buff, batchSize - n);
					}
					if(n < batchSize) {
						n += inTasksQueue.drainTo(buff, batchSize - n);
					}
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
			try(
				final Instance logCtx = CloseableThreadContext
					.put(KEY_TEST_STEP_ID, stepName)
					.put(KEY_CLASS_NAME, IoTasksDispatch.class.getSimpleName())
			) {
				if(buff.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
					buff.clear();
				} else {
					Loggers.ERR.warn(
						"{}: failed to obtain the I/O tasks buffer lock in time",
						StorageDriverBase.this.toString()
					);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(
					Level.WARN, e, "{}: interrupted on close",
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
		prepareIoTask(task);
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
			prepareIoTask(nextTask);
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
				prepareIoTask(nextIoTask);
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
	
	private void prepareIoTask(final O ioTask)
	throws ServerException {
		ioTask.reset();
		if(ioTask instanceof DataIoTask) {
			((DataIoTask) ioTask).getItem().setContentSrc(contentSrc);
		}
		if(requestAuthTokenFunc != null) {
			final Credential credential = ioTask.getCredential();
			if(credential != null) {
				authTokens.computeIfAbsent(credential, requestAuthTokenFunc);
			}
		}
		if(requestNewPathFunc != null) {
			final String dstPath = ioTask.getDstPath();
			// NOTE: in the distributed mode null dstPath becomes empty one
			if(dstPath != null && !dstPath.isEmpty()) {
				if(null == pathMap.computeIfAbsent(dstPath, requestNewPathFunc)) {
					ioTask.setStatus(IoTask.Status.FAIL_UNKNOWN);
				}
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
		if(ioResultsQueue.isEmpty()) {
			return Collections.emptyList();
		}
		final List<O> ioTaskResults = new ArrayList<>(queueCapacity);
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
		if(Loggers.MSG.isTraceEnabled()) {
			Loggers.MSG.trace("{}: I/O task completed", ioTask);
		}

		final O ioTaskResult = ioTask.getResult();
		if(!ioResultsQueue.offer(ioTaskResult/*, 1, TimeUnit.MICROSECONDS*/)) {
			Loggers.ERR.warn("{}: I/O task results queue overflow, dropping the result", toString());
		}

		if(ioTask instanceof CompositeIoTask) {
			final CompositeIoTask parentTask = (CompositeIoTask) ioTask;
			if(!parentTask.allSubTasksDone()) {
				final List<O> subTasks = parentTask.getSubTasks();
				for(final O nextSubTask : subTasks) {
					if(!childTasksQueue.offer(nextSubTask/*, 1, TimeUnit.MICROSECONDS*/)) {
						Loggers.ERR.warn(
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
				if(!childTasksQueue.offer((O) parentTask/*, 1, TimeUnit.MICROSECONDS*/)) {
					Loggers.ERR.warn(
						"{}: I/O child tasks queue overflow, dropping the I/O task", toString()
					);
				}
			}
		}
	}

	protected abstract boolean submit(final O ioTask)
	throws InterruptedException;

	protected abstract int submit(final List<O> ioTasks, final int from, final int to)
	throws InterruptedException;

	protected abstract int submit(final List<O> ioTasks)
	throws InterruptedException;

	@Override
	public Input<O> getInput() {
		return this;
	}
	
	@Override
	protected void doShutdown() {
		svcTasks.remove(ioTasksDispatchTask);
		try {
			ioTasksDispatchTask.close();
		} catch(final IOException ignored) {
		}
		Loggers.MSG.debug("{}: shut down", toString());
	}

	@Override
	protected void doInterrupt() {
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepName)
				.put(KEY_CLASS_NAME, StorageDriverBase.class.getSimpleName())
		) {
			if(!concurrencyThrottle.tryAcquire(concurrencyLevel, 10, TimeUnit.MILLISECONDS)) {
				Loggers.MSG.debug("{}: interrupting while not in the idle state", toString());
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(Level.WARN, e, "Failed to await the idle state");
		} finally {
			Loggers.MSG.debug("{}: interrupted", toString());
		}
	}

	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		super.doClose();
		try(
			final Instance logCtx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepName)
				.put(KEY_CLASS_NAME, StorageDriverBase.class.getSimpleName())
		) {
			contentSrc.close();
			childTasksQueue.clear();
			inTasksQueue.clear();
			final int ioResultsQueueSize = ioResultsQueue.size();
			if(ioResultsQueueSize > 0) {
				Loggers.ERR.warn(
					"{}: I/O results queue contains {} unhandled elements", toString(),
					ioResultsQueueSize
				);
			}
			ioResultsQueue.clear();
			pathMap.clear();
			Loggers.MSG.debug("{}: closed", toString());
		}
	}
	
	@Override
	public String toString() {
		return "storage/driver/" + concurrencyLevel + "/%s/" + hashCode();
	}
}
