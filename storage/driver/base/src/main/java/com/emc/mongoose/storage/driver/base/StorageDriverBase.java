package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.concurrent.DaemonBase;
import static com.emc.mongoose.model.io.task.IoTask.SLASH;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.common.io.collection.IoBuffer;
import com.emc.mongoose.common.io.collection.LimitedQueueBuffer;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 11.07.16.
 This mock just passes the submitted tasks to the load monitor em
 */
public abstract class StorageDriverBase<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements StorageDriver<I, O>, Runnable {

	private static final Logger LOG = LogManager.getLogger();
	private static final int BATCH_SIZE = 0x1000;
	private static final Map<String, Runnable> DISPATCH_TASKS = new ConcurrentHashMap<>();
	private static final Thread IO_TASK_DISPATCHER = new Thread("storageDriverIoTaskDispatcher") {

		{
			setDaemon(true);
			start();
			Runtime.getRuntime().addShutdownHook(
				new Thread() {
					public final void run() {
						IO_TASK_DISPATCHER.interrupt();
						DISPATCH_TASKS.clear();
					}
				}
			);
		}

		@Override
		public final void run() {
			try {
				while(true) {
					Runnable nextStorageDriverTask;
					for(final String storageDriverName : DISPATCH_TASKS.keySet()) {
						nextStorageDriverTask = DISPATCH_TASKS.get(storageDriverName);
						if(nextStorageDriverTask != null) {
							try {
								nextStorageDriverTask.run();
							} catch(final Exception e) {
								LogUtil.exception(
									LOG, Level.WARN, e,
									"Failed to invoke the I/O task dispatching for the storage " +
									"driver \"{}\"", storageDriverName
								);
							}
							Thread.sleep(1);
						}
					}
				}
			} catch(final InterruptedException e) {
				LOG.debug(Markers.MSG, "Interrupted");
			}
		}
	};

	private volatile Output<O> ioTaskOutput = null;
	private final IoBuffer<O> ioTaskBuff;
	private final boolean isCircular;
	protected final String jobName;
	protected final int concurrencyLevel;
	protected final String userName;
	protected final String secret;
	protected final boolean verifyFlag;

	protected StorageDriverBase(
		final String jobName, final AuthConfig authConfig, final LoadConfig loadConfig,
		final boolean verifyFlag
	) {
		this.ioTaskBuff = new LimitedQueueBuffer<>(
			new ArrayBlockingQueue<>(loadConfig.getQueueConfig().getSize())
		);
		this.jobName = jobName;
		this.userName = authConfig == null ? null : authConfig.getId();
		secret = authConfig == null ? null : authConfig.getSecret();
		concurrencyLevel = loadConfig.getConcurrency();
		isCircular = loadConfig.getCircular();
		this.verifyFlag = verifyFlag;
		DISPATCH_TASKS.put(toString(), this);
	}
	
	@Override
	public int getConcurrencyLevel() {
		return concurrencyLevel;
	}

	@Override
	public final void setOutput(final Output<O> ioTaskOutput)
	throws IllegalStateException {
		this.ioTaskOutput = ioTaskOutput;
	}

	// prepend the path to the item name
	private void setItemPath(final O ioTask) {
		final String dstPath = ioTask.getDstPath();
		final I item = ioTask.getItem();
		if(dstPath == null) {
			final String srcPath = ioTask.getSrcPath();
			if(srcPath != null && !srcPath.isEmpty()) {
				if(srcPath.endsWith(SLASH)) {
					item.setName(srcPath + item.getName());
				} else {
					item.setName(srcPath + SLASH + item.getName());
				}
			}
		} else {
			if(dstPath.endsWith(SLASH)) {
				item.setName(dstPath + item.getName());
			} else {
				item.setName(dstPath + SLASH + item.getName());
			}
		}
	}

	protected final void ioTaskCompleted(final O ioTask) {
		setItemPath(ioTask);
		try {
			ioTaskBuff.put(ioTask);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to put the I/O task to the output buffer"
			);
		}
	}
	
	protected final int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to) {

		for(int i = from; i < to; i++) {
			setItemPath(ioTasks.get(i));
		}

		try {
			for(int i = from; i < to; i += ioTaskBuff.put(ioTasks, i, to)) {
				LockSupport.parkNanos(1);
			}
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to put {} I/O tasks to the output buffer", to - from
			);
		}

		return to - from;
	}

	private final List<O> ioTasks = new ArrayList<>(BATCH_SIZE);
	@Override
	public final void run() {
		try {
			final int n = ioTaskBuff.get(ioTasks, BATCH_SIZE);
			if(ioTaskOutput != null) {
				for(int i = 0; i < n; i += ioTaskOutput.put(ioTasks, 0, n)) {
					LockSupport.parkNanos(1);
				}
			}
			if(isCircular) {
				for(int i = 0; i < n; i ++) {
					ioTasks.get(i).reset();
				}
				for(int i = 0; i < n; i += put(ioTasks, 0, n)) {
					LockSupport.parkNanos(1);
				}
			}
		} catch(final IOException e) {
			if(!isInterrupted() && !isClosed()) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to dispatch the completed I/O tasks");
			} // else ignore
		}
	}
	
	@Override
	public Input<O> getInput() {
		return null;
	}
	
	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		DISPATCH_TASKS.remove(toString());
		ioTaskOutput = null;
	}
	
	@Override
	public String toString() {
		return "storage/driver/%s/" + hashCode();
	}
}
