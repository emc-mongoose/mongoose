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
	public static final String
		KEY_LOAD_NUM = "load.number",
		KEY_API = "api",
		KEY_LOAD_TYPE = "load.type",
		KEY_THREAD_NUM = "thread.number";
	//
	private final int loadNumber;
	private final String api;
	private final AsyncIOTask.Type loadType;
	//
	public DataObjectWorkerFactory(
		final int loadNumber, final String api, final AsyncIOTask.Type loadType,
		final String threadNamePrefix
	) {
		super(threadNamePrefix);
		this.loadNumber = loadNumber;
		this.api = api;
		this.loadType = loadType;
	}
	//
	@Override
	public DataObjectWorker newThread(final Runnable runnable) {
		return new DataObjectWorker(
			runnable, loadNumber, api, loadType, threadNamePrefix, threadNumber.getAndIncrement()
		);
	}
	//
	private static final class DataObjectWorker
	extends Thread {
		//
		private final int loadNumber;
		private final String api;
		private final int threadNumber;
		private final AsyncIOTask.Type loadType;
		//
		private DataObjectWorker(
			final Runnable runnable,
			final int loadNumber,
			final String api,
			final AsyncIOTask.Type loadType,
			final String threadNamePrefix,
			final int threadNumber
		) {
			super(runnable, String.format(FMT_NAME_THREAD, threadNamePrefix, threadNumber));
			this.loadNumber = loadNumber;
			this.api = api;
			this.loadType = loadType;
			this.threadNumber = threadNumber;
		}
		//
		@Override
		public void run() {
			ThreadContextMap.putValue(KEY_LOAD_NUM, String.valueOf(loadNumber));
			ThreadContextMap.putValue(KEY_API, api);
			ThreadContextMap.putValue(KEY_LOAD_TYPE, loadType.toString());
			ThreadContextMap.putValue(KEY_THREAD_NUM, String.valueOf(threadNumber));
			super.run();
		}
	}
}
