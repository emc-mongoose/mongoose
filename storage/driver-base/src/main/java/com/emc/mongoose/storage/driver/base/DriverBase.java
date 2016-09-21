package com.emc.mongoose.storage.driver.base;

import com.emc.mongoose.common.concurrent.DaemonBase;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;

import com.emc.mongoose.model.api.io.Input;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.api.load.Monitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created by kurila on 11.07.16.
 This mock just passes the submitted tasks to the load monitor em
 */
public abstract class DriverBase<I extends Item, O extends IoTask<I>>
extends DaemonBase
implements Driver<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	protected final AtomicReference<Monitor<I, O>> monitorRef = new AtomicReference<>(null);
	protected final String runId;
	protected final int concurrencyLevel;
	protected final boolean isCircular;
	protected final String userName;
	protected final String secret;
	protected final String srcContainer;

	protected DriverBase(
		final String runId, final AuthConfig authConfig, final LoadConfig loadConfig,
		final String srcContainer
	) {
		this.runId = runId;
		this.userName = authConfig.getId();
		secret = authConfig.getSecret();
		concurrencyLevel = loadConfig.getConcurrency();
		isCircular = loadConfig.getCircular();
		this.srcContainer = srcContainer;
	}

	@Override
	public final void register(final Monitor<I, O> monitor)
	throws IllegalStateException {
		if(monitorRef.compareAndSet(null, monitor)) {
			monitor.register(this);
		} else {
			throw new IllegalStateException("Driver is already used by another monitor");
		}
	}
	
	public final void ioTaskCompleted(final O ioTask)
	throws IOException {
		if(isCircular) {
			ioTask.reset();
			put(ioTask);
		}
		monitorRef.get().put(ioTask);
	}
	
	public final int ioTaskCompletedBatch(final List<O> ioTasks, final int from, final int to)
	throws IOException {
		if(isCircular) {
			for(int i = from; i < to; i ++) {
				ioTasks.get(i).reset();
			}
			put(ioTasks, from, to);
		}
		return monitorRef.get().put(ioTasks, from, to);
	}
	
	@Override
	public Input<O> getInput() {
		return null;
	}
	
	@Override
	protected void doClose()
	throws IOException, IllegalStateException {
		monitorRef.set(null);
	}
}
