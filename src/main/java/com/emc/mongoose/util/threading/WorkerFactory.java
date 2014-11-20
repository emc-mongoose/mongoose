package com.emc.mongoose.util.threading;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import com.emc.mongoose.run.ThreadContextMap;

import java.util.concurrent.ThreadFactory;
/**
 Created by kurila on 25.04.14.
 */
public class WorkerFactory
implements ThreadFactory {
	//
	//private static final Logger LOG = LogManager.getLogger();
	//
	protected final String threadNamePrefix;
	private volatile int threadNumber;
	//
	public WorkerFactory(final String threadNamePrefix) {
		//LOG.trace(Markers.MSG, "New worker factory: \"{}\"", threadNamePrefix);
		this.threadNamePrefix = threadNamePrefix;
		this.threadNumber = 0;
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public Thread newThread(final Runnable runnable) {
		//LOG.trace(LogMark.MSG, "Handling new task \"{}\"", runnable.toString());
		return new Thread(
			runnable, threadNamePrefix + '#' + Integer.toString(threadNumber++)
		);
	}
	//
	@Override
	public final String toString() {
		return threadNamePrefix;
	}
}
