package com.emc.mongoose.common.io;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.net.http.IOUtils;
/**
 Created by kurila on 10.08.15.
 */
public class DirectMemIOWorkerFactory
extends GroupThreadFactory {
	//
	public DirectMemIOWorkerFactory(final String threadNamePrefix) {
		super(threadNamePrefix);
	}
	//
	protected static class DirectMemIOWorker
	extends Thread {
		//
		protected DirectMemIOWorker(final Runnable task, final String name) {
			super(task, name);
		}
		//
		@Override
		protected void finalize()
		throws Throwable {
			IOUtils.releaseUsedDirectMemory();
			super.finalize();
		}
	}
	//
	@Override
	public Thread newThread(final Runnable task) {
		return new DirectMemIOWorker(task, getName() + "#" + threadNumber.incrementAndGet());
	}
}
