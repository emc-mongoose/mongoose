package com.emc.mongoose.threading;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import com.emc.mongoose.logging.LogMark;
//
import java.util.concurrent.ThreadFactory;
/**
 Created by kurila on 25.04.14.
 */
public class WorkerFactory
implements ThreadFactory {
	//
	//private static final Logger LOG = LogManager.getLogger();
	//
	private final String threadNamePrefix;
	private int threadNumber;
	//
	public WorkerFactory(final String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
		this.threadNumber = 0;
	}
	//
	@Override
	public Thread newThread(final Runnable runnable) {
		//LOG.trace(LogMark.MSG, "Handling new task \"{}\"", runnable.toString());
		return new Thread(
			runnable, threadNamePrefix+'#'+Integer.toString(threadNumber++)
		);
	}
	//
	@Override
	public final String toString() {
		return threadNamePrefix;
	}
	//
}
