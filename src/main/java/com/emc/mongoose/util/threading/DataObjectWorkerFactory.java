package com.emc.mongoose.util.threading;

import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.run.ThreadContextMap;
/**
 * Created by olga on 12.11.14.
 */
public class DataObjectWorkerFactory
extends WorkerFactory {
	//
	private final String addr;
	private final String api;
	private final int loadNumber;
	private final AsyncIOTask.Type loadType;
	//
	private static final String
			KEY_THREAD_NUM = "thread.number",
			KEY_NODE_ADDR = "node.addr",
			KEY_LOAD_NUM = "load.number",
			KEY_LOAD_TYPE = "load.type",
			KEY_API = "api";
	private volatile int threadNumber;
	//
	public DataObjectWorkerFactory(
			final String threadNamePrefix,
			final int loadNumber,
			final String addr,
			final String api,
			final AsyncIOTask.Type loadType
	) {
		super(threadNamePrefix);
		this.loadNumber = loadNumber;
		this.addr = addr;
		this.api = api;
		this.loadType = loadType;
		this.threadNumber = 0;
	}

	@Override @SuppressWarnings("NullableProblems")
	public Thread newThread(final Runnable runnable) {
		//LOG.trace(LogMark.MSG, "Handling new task \"{}\"", runnable.toString());
		threadNumber ++;
		return new DataObjectWorkerThread(
				runnable,
				threadNamePrefix + '#' + Integer.toString(threadNumber),
				threadNumber,
				loadNumber,
				addr,
				api,
				loadType
		);
	}

	private static final class DataObjectWorkerThread
	extends Thread {
		private final int threadNumber;
		private final String addr;
		private final String api;
		private final int loadNumber;
		private final AsyncIOTask.Type loadType;
		//
		private DataObjectWorkerThread(
				final Runnable runnable,
				final String nameThread,
				final int threadNumber,
				final int loadNumber,
				final String addr,
				final String api,
				final AsyncIOTask.Type loadType
				){
			super(runnable, nameThread);
			this.threadNumber = threadNumber;
			this.loadNumber = loadNumber;
			this.addr = addr;
			this.api = api;
			this.loadType = loadType;
		}
		//
		@Override
		public void run() {
			ThreadContextMap.putValue(KEY_THREAD_NUM, String.valueOf(threadNumber));
			ThreadContextMap.putValue(KEY_LOAD_NUM, String.valueOf(loadNumber));
			ThreadContextMap.putValue(KEY_LOAD_TYPE, loadType.toString());
			ThreadContextMap.putValue(KEY_NODE_ADDR, addr);
			ThreadContextMap.putValue(KEY_API, api);
			super.run();
		}
	}
}
