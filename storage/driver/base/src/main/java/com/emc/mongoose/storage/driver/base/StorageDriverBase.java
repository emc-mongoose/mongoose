package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.concurrent.DaemonBase;
import static com.emc.mongoose.common.Constants.BATCH_SIZE;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.MetricsConfig.TraceConfig;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 11.07.16.
 */
public abstract class StorageDriverBase<I extends Item, O extends IoTask<I, R>, R extends IoResult>
extends DaemonBase
implements StorageDriver<I, O, R> {

	private static final Logger LOG = LogManager.getLogger();
	private static final Map<String, Runnable> DISPATCH_INBOUND_TASKS = new ConcurrentHashMap<>();
	private static final Thread INBOUND_IO_TASKS_DISPATCHER = new Thread(
		new CommonDispatchTask(DISPATCH_INBOUND_TASKS), "inboundIoTasksDispatcher"
	) {
		{
			setDaemon(true);
			start();
		}
	};
	
	private static final Map<String, Runnable> DISPATCH_OUTBOUND_TASKS = new ConcurrentHashMap<>();
	private static final Thread OUTBOUND_IO_TASKS_DISPATCHER = new Thread(
		new CommonDispatchTask(DISPATCH_OUTBOUND_TASKS), "outboundIoTasksDispatcher"
	) {
		{
			setDaemon(true);
			start();
		}
	};

	private volatile Output<R> ioTaskResultOutput = null;
	private final AtomicLong completedTasks = new AtomicLong(0);
	private final IoBuffer<O> ownTasksQueue;
	private final IoBuffer<O> inTasksQueue;
	private final IoBuffer<O> outTasksQueue;
	private final boolean isCircular;
	protected final String jobName;
	protected final int concurrencyLevel;
	protected final Semaphore concurrencyThrottle;
	protected final String userName;
	protected final String secret;
	protected final String authToken;
	protected final boolean verifyFlag;

	private final boolean useStorageDriverResult;
	private final boolean useStorageNodeResult;
	private final boolean useItemPathResult;
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
		final int queueCapacity = loadConfig.getQueueConfig().getSize();
		this.ownTasksQueue = new LimitedQueueBuffer<>(new ArrayBlockingQueue<>(queueCapacity));
		this.inTasksQueue = new LimitedQueueBuffer<>(new ArrayBlockingQueue<>(BATCH_SIZE));
		this.outTasksQueue = new LimitedQueueBuffer<>(new ArrayBlockingQueue<>(queueCapacity));
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
		useItemPathResult = traceConfig.getItemPath();
		useIoTypeCodeResult = traceConfig.getIoTypeCode();
		useStatusCodeResult = traceConfig.getStatusCode();
		useReqTimeStartResult = traceConfig.getReqTimeStart();
		useDurationResult = traceConfig.getDuration();
		useRespLatencyResult = traceConfig.getRespLatency();
		useDataLatencyResult = traceConfig.getDataLatency();
		useTransferSizeResult = traceConfig.getTransferSize();

		DISPATCH_INBOUND_TASKS.put(toString(), new InboundIoTasksDispatch());
		DISPATCH_OUTBOUND_TASKS.put(toString(), new OutboundIoTasksDispatch());
	}
	
	public final class InboundIoTasksDispatch
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
				}
			} catch(final IOException e) {
				if(!isInterrupted() && !isClosed()) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to dispatch the input I/O tasks");
				} // else ignore
			}
		}
	}
	
	private final class OutboundIoTasksDispatch
	implements Runnable {
		@Override
		public final void run() {
			int n;
			final List<O> ioTasks = new ArrayList<>(BATCH_SIZE);
			try {
				ioTasks.clear();
				n = outTasksQueue.get(ioTasks, BATCH_SIZE);
				if(n > 0) {
					final List<R> ioTaskResults = new ArrayList<>(n);
					buildResults(ioTasks, ioTaskResults, n);
					if(ioTaskResultOutput != null) {
						for(int i = 0; i < n; i += ioTaskResultOutput.put(ioTaskResults, i, n)) {
							LockSupport.parkNanos(1);
						}
					}
				} else {
					LockSupport.parkNanos(1);
				}
			} catch(final IOException e) {
				if(!isInterrupted() && !isClosed()) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to dispatch the completed I/O tasks"
					);
				} // else ignore
			}
		}
	}
	
	
	
	@Override
	public final void put(final O task)
	throws IOException {
		inTasksQueue.put(task);
	}

	@Override
	public final int put(final List<O> tasks, final int from, final int to)
	throws IOException {
		return inTasksQueue.put(tasks, from, to);
	}

	@Override
	public final int put(final List<O> tasks)
	throws IOException {
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
	public final void setOutput(final Output<R> ioTaskResultOutput)
	throws IllegalStateException {
		this.ioTaskResultOutput = ioTaskResultOutput;
	}

	@SuppressWarnings("unchecked")
	protected final void ioTaskCompleted(final O ioTask) {

		try {
			if(isCircular) {
				ownTasksQueue.put(ioTask);
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
			outTasksQueue.put(ioTask);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to put the I/O task to the output buffer"
			);
		}

		completedTasks.incrementAndGet();
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

	protected abstract void submit(final O task)
	throws InterruptedIOException;

	protected abstract int submit(final List<O> tasks, final int from, final int to)
	throws InterruptedIOException;

	protected abstract int submit(final List<O> tasks)
	throws InterruptedIOException;

	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private void buildResults(final List<O> ioTasks, final List<R> ioResults, final int n) {
		if(n > 0) {
			for(int i = 0; i < n; i ++) {
				ioResults.add(
					ioTasks.get(i).getResult(
						HOST_ADDR,
						useStorageDriverResult,
						useStorageNodeResult,
						useItemPathResult,
						useIoTypeCodeResult,
						useStatusCodeResult,
						useReqTimeStartResult,
						useDurationResult,
						useRespLatencyResult,
						useDataLatencyResult,
						useTransferSizeResult
					)
				);
			}
		}
	}

	@Override
	public Input<O> getInput() {
		return null;
	}
	
	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		DISPATCH_INBOUND_TASKS.remove(toString());
		ioTaskResultOutput = null;
		ownTasksQueue.close();
		inTasksQueue.close();
		outTasksQueue.close();
	}
	
	@Override
	public String toString() {
		return "storage/driver/%s/" + hashCode();
	}
}
