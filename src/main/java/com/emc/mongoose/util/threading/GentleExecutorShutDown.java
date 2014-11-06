package com.emc.mongoose.util.threading;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.MessageFactoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 23.10.14.
 */
public final class GentleExecutorShutDown
implements Runnable {
	//
	private final Logger log;
	private final static String
		MSG_INTERRUPTED = "Interrupted externally, forcing the executor shutdown",
		MSG_FMT_DROPPED = "Dropped {} tasks",
		MSG_FMT_STATUS = "Waiting for tasks to complete: {} pending, workers: {} active / {} total";
	//
	private final ThreadPoolExecutor executor;
	private final int retryDelayMilliSec;
	//
	public GentleExecutorShutDown(
		final ThreadPoolExecutor executor, final RunTimeConfig runTimeConfig
	) {
		this.executor = executor;
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		//
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
	}
	//
	@Override
	public final void run() {
		executor.shutdown();
		//
		final BlockingQueue<Runnable> submQueue = executor.getQueue();
		final int submExecCoreThreads = executor.getCorePoolSize();
		int submQueueSize, submExecActiveThreads;
		try {
			do {
				submQueueSize = submQueue.size();
				submExecActiveThreads = executor.getActiveCount();
				log.trace(
						Markers.MSG, MSG_FMT_STATUS,
						submQueueSize, submExecActiveThreads, submExecCoreThreads
				);
			} while(
				(submQueueSize > 0 || submExecCoreThreads == submExecActiveThreads)
					&&
				!executor.isTerminated()
					&&
				!executor.awaitTermination(retryDelayMilliSec, TimeUnit.MILLISECONDS)
			);
		} catch(final InterruptedException e) {
			log.debug(Markers.MSG, MSG_INTERRUPTED);
		}
		//
		final int droppedTaskCount = executor.shutdownNow().size();
		if(droppedTaskCount > 0) {
			log.debug(Markers.MSG, MSG_FMT_DROPPED, droppedTaskCount);
		}
	}
}
