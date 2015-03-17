package com.emc.mongoose.core.impl.load.executor.util;
//
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
//
import org.apache.logging.log4j.ThreadContext;
/**
 * Created by olga on 12.11.14.
 */
public final class DataObjectWorkerFactory
extends NamingWorkerFactory {
	//
	public static final String
		KEY_LOAD_NUM = "load.number",
		KEY_API = "api",
		KEY_LOAD_TYPE = "load.type",
		KEY_THREAD_NUM = "thread.number";
	//
	private final int loadNumber;
	private final String api;
	private final IOTask.Type loadType;
	//
	public DataObjectWorkerFactory(
		final int loadNumber, final String api, final IOTask.Type loadType,
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
		private final IOTask.Type loadType;
		//
		private DataObjectWorker(
			final Runnable runnable,
			final int loadNumber,
			final String api,
			final IOTask.Type loadType,
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
			ThreadContext.put(KEY_LOAD_NUM, String.valueOf(loadNumber));
			ThreadContext.put(KEY_API, api);
			ThreadContext.put(KEY_LOAD_TYPE, loadType.toString());
			ThreadContext.put(KEY_THREAD_NUM, String.valueOf(threadNumber));
			super.run();
		}
	}
}
