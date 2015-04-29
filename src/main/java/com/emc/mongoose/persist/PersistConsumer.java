package com.emc.mongoose.persist;

import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 29.04.15.
 */
public class PersistConsumer
extends ThreadPoolExecutor
implements Runnable{
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private static final int
		POOL_SIZE = RunTimeConfig.getContext().getPersistExecutorPoolSize(),
		POOL_TIMEOUT = RunTimeConfig.getContext().getPersistExecutorPoolTimeout(),
		QUEUE_SIZE = RunTimeConfig.getContext().getPersistExecutorQueueSize();
	//
	public final static PersistDAO persistDAO = new PersistDAO();
	private final RunHolder runHolder;
	//
	public final static String
		MSG = "msg",
		ERR = "err",
		DATA_LIST = "dataList",
		PERF_AVG = "perfAvg",
		PERF_SUM = "perfSum",
		PERF_TRACE = "perfTrace";
	//
	public PersistConsumer(final RunHolder runHolder){
		super(
			POOL_SIZE, POOL_SIZE, POOL_TIMEOUT, TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(QUEUE_SIZE),
			new NamingWorkerFactory("persistWorkers")
		);
		this.runHolder = runHolder;
	}
	//
	public void submit(final PersistEvent event){
		super.submit(new WorkerTask(event));
	}
	//
	@Override
	public void run(){
		System.out.println("consumer run");
		while (!isShutdown()){
			submit(runHolder.getEvent());
		}
		System.out.println("shutdown");
		shutdown();
	}
	//
	@Override
	public final void shutdown() {
		if (!super.isShutdown()){
			super.shutdown();
		}
		if (!super.isTerminated()) {
			try {
				super.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException e) {
				LogUtil.failure(LOG, Level.ERROR, e, "Interrupted waiting for submit executor to finish");
			}
		}
		persistDAO.closeEntityMF();
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// WorkerTask
	///////////////////////////////////////////////////////////////////////////////////////////////////
	private final static class WorkerTask
		implements Runnable {
		//
		private final PersistEvent event;
		//
		public WorkerTask(final PersistEvent event){
			this.event = event;
		}
		//
		@Override
		public final void run() {
			switch (event.getMarker().getName()){
				case (MSG):
				case(ERR):
					persistDAO.persistMessage(event);
					break;
			}
		}
	}
}
