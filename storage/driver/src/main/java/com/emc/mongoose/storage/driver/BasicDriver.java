package com.emc.mongoose.storage.driver;

import com.emc.mongoose.common.concurrent.LifeCycleBase;
import com.emc.mongoose.common.exception.UserShootItsFootException;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.api.load.Monitor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created by kurila on 11.07.16.
 This mock just passes the submitted tasks to the load monitor em
 */
public class BasicDriver<I extends Item, O extends IoTask<I>>
extends LifeCycleBase
implements Driver<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	private final AtomicReference<Monitor<I, O>> monitorRef = new AtomicReference<>(null);

	public BasicDriver() {
	}

	@Override
	public final void registerMonitor(final Monitor<I, O> monitor)
	throws IllegalStateException {
		if(monitorRef.compareAndSet(null, monitor)) {
			monitor.registerDriver(this);
		} else {
			throw new IllegalStateException("Driver is already used by another monitor");
		}
	}

	@Override
	public boolean isFullThrottleEntered() {
		return true;
	}

	@Override
	public boolean isFullThrottleExited() {
		return false;
	}

	@Override
	public boolean submit(final O task) {
		final Monitor<I, O> monitor = monitorRef.get();
		if(monitor != null) {
			monitor.ioTaskCompleted(task);
		}
		return true;
	}

	@Override
	public int submit(final List<O> tasks, final int from, final int to) {
		for(int i = from; i < to; i ++) {
			tasks.get(i).setStatus(IoTask.Status.SUCC);
		}
		final Monitor<I, O> monitor = monitorRef.get();
		if(monitor != null) {
			monitor.ioTaskCompletedBatch(tasks, from, to);
		}
		return to - from;
	}

	@Override
	protected void doStart() {
	}

	@Override
	protected void doShutdown() {
	}

	@Override
	protected void doInterrupt() {
	}

	@Override
	public boolean await()
	throws InterruptedException {
		return false;
	}

	@Override
	public boolean await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		return false;
	}

	@Override
	public void close()
	throws IOException {
		if(!isInterrupted()) {
			try {
				interrupt();
			} catch(final UserShootItsFootException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to interrupt");
			}
		}
	}
}
