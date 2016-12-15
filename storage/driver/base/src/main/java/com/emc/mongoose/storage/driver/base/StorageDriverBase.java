package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.concurrent.DaemonBase;
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
import com.emc.mongoose.common.io.collection.IoBuffer;
import com.emc.mongoose.common.io.collection.LimitedQueueBuffer;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 11.07.16.
 */
public abstract class StorageDriverBase<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends DaemonBase
implements StorageDriver<I, O, R> {

	private static final Logger LOG = LogManager.getLogger();
	private static final Map<StorageDriver, Runnable> DISPATCH_INBOUND_TASKS = new ConcurrentHashMap<>();
	static {
		new Thread(new CommonDispatchTask(DISPATCH_INBOUND_TASKS), "ioTasksDispatcher") {
			{
				setDaemon(true);
				start();
			}
		};
	}

	private final int queueCapacity;
	private final IoBuffer<O> ownTasksQueue;
	private final IoBuffer<O> inTasksQueue;
	private final IoBuffer<R> ioResultsQueue;
	private final boolean isCircular;
	protected final String jobName;
	protected final int concurrencyLevel;
	protected final Semaphore concurrencyThrottle;
	protected final String userName;
	protected final String secret;
	private final String authToken;
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

	protected StorageDriverBase(
		final String jobName, final AuthConfig authConfig, final LoadConfig loadConfig,
		final boolean verifyFlag
	) {
		queueCapacity = loadConfig.getQueueConfig().getSize();
		this.ownTasksQueue = new LimitedQueueBuffer<>(new ArrayBlockingQueue<>(queueCapacity));
		this.inTasksQueue = new LimitedQueueBuffer<>(new ArrayBlockingQueue<>(BATCH_SIZE));
		this.ioResultsQueue = new LimitedQueueBuffer<>(new ArrayBlockingQueue<>(queueCapacity));
		this.jobName = jobName;
		this.userName = authConfig == null ? null : authConfig.getId();
		secret = authConfig == null ? null : authConfig.getSecret();
		authToken = authConfig == null ? null : authConfig.getToken();
		concurrencyLevel = loadConfig.getConcurrency();
		concurrencyThrottle = new Semaphore(concurrencyLevel);
		isCircular = loadConfig.getCircular();
		this.verifyFlag = verifyFlag;

		final TraceConfig traceConfig = loadConfig.getMetricsConfig().getTraceConfig();
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

		DISPATCH_INBOUND_TASKS.put(this, new IoTasksDispatch());
	}

	private final class IoTasksDispatch
	implements Runnable {
		@Override
		public final void run() {
			int n;
			final List<O> ioTasks = new ArrayList<>(BATCH_SIZE);
			try {
				ioTasks.clear();
				n = ownTasksQueue.get(ioTasks, BATCH_SIZE);
				if(n < BATCH_SIZE) {
					n += inTasksQueue.get(ioTasks, BATCH_SIZE - n);
				}
				if(n > 0) {
					for(int i = 0; i < n; i += submit(ioTasks, i, n)) {
						LockSupport.parkNanos(1);
					}
				} else {
					Thread.sleep(1);
				}
			} catch(final IOException e) {
				if(!isInterrupted() && !isClosed()) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to dispatch the input I/O tasks");
				}
			} catch(final InterruptedException ignored) {
			}
		}
	}
	
	@Override
	public final void put(final O task)
	throws IOException {
		if(!isStarted()) {
			throw new EOFException();
		}
		inTasksQueue.put(task);
	}

	@Override
	public final int put(final List<O> tasks, final int from, final int to)
	throws IOException {
		if(!isStarted()) {
			throw new EOFException();
		}
		return inTasksQueue.put(tasks, from, to);
	}

	@Override
	public final int put(final List<O> tasks)
	throws IOException {
		if(!isStarted()) {
			throw new EOFException();
		}
		return inTasksQueue.put(tasks);
	}
	
	@Override
	public int getActiveTaskCount() {
		return concurrencyLevel - concurrencyThrottle.availablePermits();
	}

	@Override
	public final boolean isIdle() {
		return !concurrencyThrottle.hasQueuedThreads() &&
			concurrencyThrottle.availablePermits() == concurrencyLevel;
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
		ioResultsQueue.get(ioTaskResults, queueCapacity);
		return ioTaskResults;
	}

	@SuppressWarnings("unchecked")
	protected final void ioTaskCompleted(final O ioTask) {

		try {
			if(isCircular) {
				if(IoTask.Status.SUCC.equals(ioTask.getStatus())) {
					ownTasksQueue.put(ioTask);
				}
			} else if(ioTask instanceof CompositeIoTask) {
				final CompositeIoTask parentTask = (CompositeIoTask) ioTask;
				if(!parentTask.allSubTasksDone()) {
					final List<O> subTasks = parentTask.getSubTasks();
					final int n = subTasks.size();
					for(int i = 0; i < n; i += ownTasksQueue.put(subTasks, i, n)) {
						LockSupport.parkNanos(1);
					}
				}
			} else if(ioTask instanceof PartialIoTask) {
				final PartialIoTask subTask = (PartialIoTask) ioTask;
				final CompositeIoTask parentTask = subTask.getParent();
				if(parentTask.allSubTasksDone()) {
					// execute once again to finalize the things if necessary:
					// complete the multipart upload, for example
					ownTasksQueue.put((O) parentTask);
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to enqueue the I/O task for the next execution"
			);
		}

		try {
			ioResultsQueue.put(
				ioTask.getResult(
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
				)
			);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to put the I/O task to the output buffer"
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
	protected void doShutdown() {
		DISPATCH_INBOUND_TASKS.remove(this);
	}

	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		ownTasksQueue.close();
		inTasksQueue.close();
		ioResultsQueue.close();
	}
	
	@Override
	public String toString() {
		return "storage/driver/%s/" + hashCode();
	}
}
