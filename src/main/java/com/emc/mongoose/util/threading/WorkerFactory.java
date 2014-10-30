package com.emc.mongoose.util.threading;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
import com.emc.mongoose.base.api.RequestConfig;
import org.apache.logging.log4j.ThreadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
/**
 Created by kurila on 25.04.14.
 */
public class WorkerFactory
implements ThreadFactory {
	//
	//private static final Logger LOG = LogManager.getLogger();
	//
	private final String threadNamePrefix,
						KEY_THREAD_NUM = "thread.number";
	private volatile int threadNumber;
	private final Map<String,String> context;
	//
	public WorkerFactory(final String threadNamePrefix, final Map<String,String> context) {
		//LOG.trace(Markers.MSG, "New worker factory: \"{}\"", threadNamePrefix);
		this.threadNamePrefix = threadNamePrefix;
		this.threadNumber = 0;
		this.context = context;

	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public Thread newThread(final Runnable runnable) {
		//LOG.trace(LogMark.MSG, "Handling new task \"{}\"", runnable.toString());
		threadNumber ++;
		context.put(KEY_THREAD_NUM,Integer.toString(threadNumber));
		return new WorkerThread(
			runnable, threadNamePrefix + '#' + Integer.toString(threadNumber),context
		);
	}
	//
	@Override
	public final String toString() {
		return threadNamePrefix;
	}
	//
	private static final class WorkerThread
	extends Thread{
		private final Map<String,String> context;
		//
		private WorkerThread(final Runnable runnable, final String nameThread, final Map<String,String> contex){
			super(runnable,nameThread);
			this.context = contex;
		}
		//
		@Override
		public void run() {
			for(String key: context.keySet()){
				ThreadContext.put(key,context.get(key));
			}
			super.run();
		}
	}

}
