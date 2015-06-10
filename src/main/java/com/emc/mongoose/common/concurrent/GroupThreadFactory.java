package com.emc.mongoose.common.concurrent;
//
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.common.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 25.04.14.
 */
public class GroupThreadFactory
extends ThreadGroup
implements ThreadFactory {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	protected final AtomicInteger threadNumber = new AtomicInteger(0);
	//
	public GroupThreadFactory(final String threadNamePrefix) {
		this(threadNamePrefix, false);
	}
	//
	public GroupThreadFactory(final String threadNamePrefix, final boolean isDaemon) {
		super(Thread.currentThread().getThreadGroup(), threadNamePrefix);
		setDaemon(isDaemon);
	}
	//
	@Override
	public Thread newThread(final Runnable task) {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Handling new task \"{}\"", task.toString());
		}
		return new Thread(task, getName() + "#" + threadNumber.incrementAndGet());
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
	//
	@Override
	public final void uncaughtException(final Thread thread, final Throwable thrown) {
		LogUtil.exception(
			LOG, Level.DEBUG, thrown, "Thread \"{}\" terminated because of the exception"
		);
	}
}
