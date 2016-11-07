package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.concurrent.DaemonBase;
import static com.emc.mongoose.model.io.task.IoTask.SLASH;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import com.emc.mongoose.model.io.Input;
import com.emc.mongoose.model.io.Output;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created by kurila on 11.07.16.
 This mock just passes the submitted tasks to the load monitor em
 */
public abstract class StorageDriverBase<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements StorageDriver<I, O> {

	private static final Logger LOG = LogManager.getLogger();

	protected final AtomicReference<Output<O>> ioTaskOutputRef = new AtomicReference<>(null);
	protected final String jobName;
	protected final int concurrencyLevel;
	protected final boolean isCircular;
	protected final String userName;
	protected final String secret;
	protected final boolean verifyFlag;

	protected StorageDriverBase(
		final String jobName, final AuthConfig authConfig, final LoadConfig loadConfig,
		final boolean verifyFlag
	) {
		this.jobName = jobName;
		this.userName = authConfig == null ? null : authConfig.getId();
		secret = authConfig == null ? null : authConfig.getSecret();
		concurrencyLevel = loadConfig.getConcurrency();
		isCircular = loadConfig.getCircular();
		this.verifyFlag = verifyFlag;
	}
	
	@Override
	public int getConcurrencyLevel() {
		return concurrencyLevel;
	}

	@Override
	public final void setOutput(final Output<O> ioTaskOutput)
	throws IllegalStateException {
		if(!ioTaskOutputRef.compareAndSet(null, ioTaskOutput)) {
			throw new IllegalStateException(
				"This storage driver is already used by another monitor"
			);
		}
	}

	protected final void ioTaskCompleted(final O ioTask) {

		// prepend the path to the item name
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

		if(isCircular) {
			ioTask.reset();
			try {
				put(ioTask);
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to reschedule the I/O task");
			}
		}
		
		final Output<O> ioTaskOutput = ioTaskOutputRef.get();
		if(ioTaskOutput != null) {
			try {
				ioTaskOutput.put(ioTask);
			} catch(final NoSuchObjectException | EOFException e) {
				if(isClosed() || isInterrupted()) {
					// ignore
				} else {
					LogUtil.exception(LOG, Level.WARN, e, "Lost the connection with the monitor");
				}
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to send the I/O task back to the monitor"
				);
			}
		}
	}
	
	protected final int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to) {

		String dstPath;
		O ioTask;
		I item;

		if(isCircular) {
			
			for(int i = from; i < to; i++) {
				ioTask = ioTasks.get(i);
				dstPath = ioTask.getDstPath();
				item = ioTask.getItem();
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
				ioTask.reset();
			}

			try {
				put(ioTasks, from, to);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to reschedule {} I/O tasks", to - from
				);
			}
			
		} else {
			
			for(int i = from; i < to; i++) {
				ioTask = ioTasks.get(i);
				dstPath = ioTask.getDstPath();
				item = ioTask.getItem();
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
		}

		final Output<O> ioTaskOutput = ioTaskOutputRef.get();
		if(ioTaskOutput != null) {
			try {
				return ioTaskOutput.put(ioTasks, from, to);
			} catch(final NoSuchObjectException | EOFException e) {
				if(isClosed() || isInterrupted()) {
					// ignore
				} else {
					LogUtil.exception(LOG, Level.WARN, e, "Lost the connection with the monitor");
				}
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to send {} I/O tasks back to the monitor", to - from
				);
			}
			return 0;
		} else {
			return 0;
		}
	}
	
	@Override
	public Input<O> getInput() {
		return null;
	}
	
	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		ioTaskOutputRef.set(null);
	}
	
	@Override
	public String toString() {
		return "storage/driver/%s/" + jobName;
	}
}
