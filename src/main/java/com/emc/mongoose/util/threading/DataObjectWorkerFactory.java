package com.emc.mongoose.util.threading;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.run.ThreadContextMap;
/**
 * Created by olga on 12.11.14.
 */
public final class DataObjectWorkerFactory
extends WorkerFactory {
	//
	private final String api;
	private final int loadNumber;
	private final AsyncIOTask.Type loadType;
	//
	private static final String
			KEY_THREAD_NUM = "thread.number",
			KEY_LOAD_NUM = "load.number",
			KEY_LOAD_TYPE = "load.type",
			KEY_API = "api";
	private volatile int threadNumber;
	//
	public DataObjectWorkerFactory(
		final String threadNamePrefix,
		final int loadNumber,
		final String api,
		final AsyncIOTask.Type loadType
	) {
		super(threadNamePrefix);
		this.loadNumber = loadNumber;
		this.api = api;
		this.loadType = loadType;
		this.threadNumber = 0;
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public DataObjectWorker newThread(final Runnable runnable) {
		//LOG.trace(LogMark.MSG, "Handling new task \"{}\"", runnable.toString());
		threadNumber ++;
		return new DataObjectWorker(
			runnable,
			threadNamePrefix + '#' + Integer.toString(threadNumber),
			threadNumber,
			loadNumber,
			api,
			loadType
		);
	}
	//
	private static final class DataObjectWorker
	extends Thread {
		private final int threadNumber;
		private final String api;
		private final int loadNumber;
		private final AsyncIOTask.Type loadType;
		//
		private DataObjectWorker(
			final Runnable runnable,
			final String nameThread,
			final int threadNumber,
			final int loadNumber,
			final String api,
			final AsyncIOTask.Type loadType
		){
			super(runnable, nameThread);
			this.threadNumber = threadNumber;
			this.loadNumber = loadNumber;
			this.api = api;
			this.loadType = loadType;
		}
		//
		@Override
		public void run() {
			ThreadContextMap.putValue(KEY_THREAD_NUM, String.valueOf(threadNumber));
			ThreadContextMap.putValue(KEY_LOAD_NUM, String.valueOf(loadNumber));
			ThreadContextMap.putValue(KEY_LOAD_TYPE, loadType.toString());
			ThreadContextMap.putValue(KEY_API, api);
			super.run();
		}
	}
}
