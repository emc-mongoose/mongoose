package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.model.DaemonBase;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.MetricsConfig.TraceConfig;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.LoadConfig.MetricsConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
/**
 Created by kurila on 11.07.16.
 */
public abstract class StorageDriverBase<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends DaemonBase
implements StorageDriver<I, O, R> {

	private static final Logger LOG = LogManager.getLogger();
	
	private final int queueCapacity;
	private final BlockingQueue<O> childTasksQueue;
	private final BlockingQueue<O> inTasksQueue;
	private final BlockingQueue<R> ioResultsQueue;
	private final boolean isCircular;
	protected final String jobName;
	protected final int concurrencyLevel;
	protected final Semaphore concurrencyThrottle;
	protected final String userName;
	protected final String secret;
	protected volatile String authToken;
	protected final boolean verifyFlag;

	private final boolean useStorageDriverResult;
	private final boolean useStorageNodeResult;
	private final boolean useItemInfoResult;
	private final boolean useIoTypeCodeResult;
	private final boolean useStatusCodeResult;
	private final boolean useReqTimeStartResult;
	private final boolean useDurationResult;
	private final boolean useRespLatencyResult;
	private final boolean useDataLatencyResult;
	private final boolean useTransferSizeResult;
	
	private final LongAdder scheduledTaskCount = new LongAdder();
	private final LongAdder completedTaskCount = new LongAdder();
	private final LongAdder recycledTaskCount = new LongAdder();

	protected StorageDriverBase(
		final String jobName, final AuthConfig authConfig, final LoadConfig loadConfig,
		final boolean verifyFlag
	) {
		queueCapacity = loadConfig.getQueueConfig().getSize();
		this.childTasksQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.inTasksQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.ioResultsQueue = new ArrayBlockingQueue<>(queueCapacity);
		this.jobName = jobName;
		this.userName = authConfig == null ? null : authConfig.getId();
		secret = authConfig == null ? null : authConfig.getSecret();
		authToken = authConfig == null ? null : authConfig.getToken();
		concurrencyLevel = loadConfig.getConcurrency();
		concurrencyThrottle = new Semaphore(concurrencyLevel, true);
		isCircular = loadConfig.getCircular();
		this.verifyFlag = verifyFlag;

		final MetricsConfig metricsConfig = loadConfig.getMetricsConfig();
		final TraceConfig traceConfig = metricsConfig.getTraceConfig();
		useStorageDriverResult = traceConfig.getStorageDriver();
		useStorageNodeResult = traceConfig.getStorageNode();
		useItemInfoResult = traceConfig.getItemInfo();
		useIoTypeCodeResult = traceConfig.getIoTypeCode();
		useStatusCodeResult = traceConfig.getStatusCode();
		useReqTimeStartResult = traceConfig.getReqTimeStart();
		useDurationResult = traceConfig.getDuration();
		useRespLatencyResult = traceConfig.getRespLatency();
		useDataLatencyResult = traceConfig.getDataLatency();
		useTransferSizeResult = traceConfig.getTransferSize();

		SVC_TASKS.put(this, new IoTasksDispatch());
	}

	private final class IoTasksDispatch
	implements Runnable {

		final List<O> ioTasks = new ArrayList<>(BATCH_SIZE);
		final List<O> prevIoTasks = new ArrayList<>(BATCH_SIZE);
		int n;
		int m;

		@Override
		public final void run() {
			ioTasks.addAll(prevIoTasks);
			prevIoTasks.clear();
			n = ioTasks.size();
			if(n < BATCH_SIZE) {
				n += childTasksQueue.drainTo(ioTasks, BATCH_SIZE - n);
			}
			if(n < BATCH_SIZE) {
				n += inTasksQueue.drainTo(ioTasks, BATCH_SIZE - n);
			}
			try {
				if(n > 0) {
					m = submit(ioTasks, 0, n);
					if(m < n) {
						prevIoTasks.addAll(ioTasks.subList(m, n));
					}
					ioTasks.clear();
				}
			} catch(final InterruptedException e) {
				SVC_TASKS.clear();
			}
		}
	}

	@Override
	public final boolean put(final O task)
	throws EOFException {
		if(!isStarted()) {
			throw new EOFException();
		}
		if(isCircular && scheduledTaskCount.sum() >= queueCapacity) {
			throw new EOFException();
		}
		if(inTasksQueue.offer(task)) {
			scheduledTaskCount.increment();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public final int put(final List<O> tasks, final int from, final int to)
	throws EOFException {
		int j;
		if(isCircular) {
			final long remaining = queueCapacity - scheduledTaskCount.sum();
			if(remaining < 1) {
				throw new EOFException();
			}
			j = (int) (from + Math.min(to - from, remaining));
		} else {
			j = to;
		}
		int i = from;
		while(i < j && isStarted()) {
			if(inTasksQueue.offer(tasks.get(i))) {
				i ++;
			} else {
				break;
			}
		}
		final int n = i - j;
		scheduledTaskCount.add(n);
		return n;
	}

	@Override
	public final int put(final List<O> tasks)
	throws EOFException {
		if(isCircular) {
			final long remaining = queueCapacity - scheduledTaskCount.sum();
			if(remaining < 1) {
				throw new EOFException();
			}
			if(remaining < tasks.size()) {
				return put(tasks, 0, (int) remaining);
			}
		}
		int n = 0;
		for(final O nextIoTask : tasks) {
			if(isStarted() && inTasksQueue.offer(nextIoTask)) {
				n ++;
			} else {
				break;
			}
		}
		scheduledTaskCount.add(n);
		return n;
	}
	
	@Override
	public int getActiveTaskCount() {
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
	public final long getRecycledTaskCount() {
		return recycledTaskCount.sum();
	}

	@Override
	public final boolean isIdle() {
		return !concurrencyThrottle.hasQueuedThreads() &&
			concurrencyThrottle.availablePermits() >= concurrencyLevel;
	}

	@Override
	public final boolean isFullThrottleEntered() {
		// TODO use full load threshold
		return concurrencyThrottle.availablePermits() == 0;
	}

	@Override
	public final boolean isFullThrottleExited() {
		// TODO use full load threshold
		return isShutdown() && concurrencyThrottle.availablePermits() > 0;
	}

	@Override
	public List<R> getResults()
	throws IOException {
		final List<R> ioTaskResults = new ArrayList<>(BATCH_SIZE);
		ioResultsQueue.drainTo(ioTaskResults, queueCapacity);
		return ioTaskResults;
	}

	@SuppressWarnings("unchecked")
	protected final void ioTaskCompleted(final O ioTask) {

		completedTaskCount.increment();

		try {
			if(isCircular) {
				if(IoTask.Status.SUCC.equals(ioTask.getStatus())) {
					if(inTasksQueue.offer(ioTask, 1, TimeUnit.MILLISECONDS)) {
						recycledTaskCount.increment();
					} else {
						LOG.warn(
							Markers.ERR,
							"{}: incoming I/O tasks queue overflow, dropping the I/O task",
							toString()
						);
					}
				}
			} else if(ioTask instanceof CompositeIoTask) {
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
							Markers.ERR, "{}: I/O child tasks queue overflow, dropping the I/O task",
							toString()
						);
					}
				}
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Interrupted the completed I/O task processing");
		}

		try {
			final R ioResult = ioTask.getResult(
				HOST_ADDR,
				useStorageDriverResult,
				useStorageNodeResult,
				useItemInfoResult,
				useIoTypeCodeResult,
				useStatusCodeResult,
				useReqTimeStartResult,
				useDurationResult,
				useRespLatencyResult,
				useDataLatencyResult,
				useTransferSizeResult
			);
			if(!ioResultsQueue.offer(ioResult, 1, TimeUnit.MILLISECONDS)) {
				LOG.warn(
					Markers.ERR, "{}: I/O task results queue overflow, dropping the result",
					toString()
				);
			}
		} catch(final InterruptedException e) {
			LogUtil.exception(
				LOG, Level.DEBUG, e, "Interrupting the I/O task put to the output buffer"
			);
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

	protected abstract boolean submit(final O task)
	throws InterruptedException;

	protected abstract int submit(final List<O> tasks, final int from, final int to)
	throws InterruptedException;

	protected abstract int submit(final List<O> tasks)
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
		LOG.info(Markers.MSG, "{}: shut down", toString());
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
			LOG.info(Markers.MSG, "{}: interrupted", toString());
		}
	}

	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		childTasksQueue.clear();
		inTasksQueue.clear();
		ioResultsQueue.clear();
		LOG.info(Markers.MSG, "{}: closed", toString());
	}
	
	@Override
	public String toString() {
		return "storage/driver/" + concurrencyLevel + "/%s/" + hashCode();
	}
}
