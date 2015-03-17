package com.emc.mongoose.common.concurrent;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 25.04.14.
 */
public class NamingWorkerFactory
implements ThreadFactory {
	//
	//private static final Logger LOG = LogManager.getLogger();
	protected static final String FMT_NAME_THREAD = "%s#%s";
	//
	protected final String threadNamePrefix;
	protected final AtomicInteger threadNumber = new AtomicInteger(0);
	//
	public NamingWorkerFactory(final String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}
	//
	@Override
	public Thread newThread(final Runnable runnable) {
		//LOG.trace(LogMark.MSG, "Handling new task \"{}\"", runnable.toString());
		return new Thread(
			runnable,
			String.format(FMT_NAME_THREAD, threadNamePrefix, threadNumber.incrementAndGet())
		);
	}
	//
	@Override
	public final String toString() {
		return threadNamePrefix;
	}
}
