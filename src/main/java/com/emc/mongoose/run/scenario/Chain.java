package com.emc.mongoose.run.scenario;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.data.model.DataItemFileSrc;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.data.model.ItemCSVFileDst;
import com.emc.mongoose.core.impl.load.tasks.AwaitAndCloseLoadJobTask;
//
import com.emc.mongoose.util.builder.LoadBuilderFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 11.06.15.
 */
public class Chain
implements Runnable {
	//
	private final static Logger LOG;
	static {
		LogUtil.init();
		LOG = LogManager.getLogger();
	}
	//
	private final List<LoadExecutor> loadJobSeq = new LinkedList<>();
	private final long timeOut;
	private final TimeUnit timeUnit;
	private final boolean isParallel;
	//
	private volatile boolean interrupted;
	//
	public Chain(final RunTimeConfig rtConfig) {
		this(
			(DataLoadBuilder) LoadBuilderFactory.getInstance(rtConfig),
			rtConfig.getLoadLimitTimeValue(), rtConfig.getLoadLimitTimeUnit(),
			rtConfig.getScenarioChainLoad(), rtConfig.getScenarioChainConcurrentFlag()
		);
	}
	//
	@SuppressWarnings("unchecked")
	public Chain(
		final DataLoadBuilder loadBuilder, final long timeOut, final TimeUnit timeUnit,
		final String[] loadTypeSeq, final boolean isParallel
	) {
		this.timeOut = timeOut > 0 ? timeOut : Long.MAX_VALUE;
		this.timeUnit = timeOut > 0 ? timeUnit : TimeUnit.DAYS;
		this.isParallel = isParallel;
		//
		String loadTypeStr;
		LoadExecutor nextLoadJob, prevLoadJob = null;
		final RequestConfig reqConf;
		try {
			reqConf = loadBuilder.getRequestConfig();
		} catch(final RemoteException e) {
			throw new RuntimeException(e);
		}
		ItemDst itemDst = null;
		IOTask.Type loadType;
		for(int i = 0; i < loadTypeSeq.length; i ++) {
			loadTypeStr = loadTypeSeq[i];
			loadType = IOTask.Type.valueOf(loadTypeStr.toUpperCase());
			LOG.debug(Markers.MSG, "Building the load job #{}, type is \"{}\"", i, loadTypeStr);
			try {
				loadBuilder.setLoadType(loadType);
				// determine the items source for the next load job
				if(prevLoadJob == null) {
					if(IOTask.Type.CREATE.equals(loadType)) {
						loadBuilder.useNewItemSrc();
					} else {
						loadBuilder.useContainerListingItemSrc();
					}
				} else {
					if(isParallel) {
						loadBuilder.useNoneItemSrc();
					} else {
						itemDst = new ItemCSVFileDst(
							reqConf.getItemClass(), reqConf.getContentSource()
						);
						loadBuilder.setItemSrc(itemDst.getItemSrc());
					}
				}
				// build the job
				nextLoadJob = loadBuilder.build();
				// determine the items destination for the next load job
				if(prevLoadJob != null) {
					if(isParallel) {
						itemDst = nextLoadJob;
					}
					prevLoadJob.setItemDst(itemDst);
				}
				// add the built job into the chain
				loadJobSeq.add(nextLoadJob);
				prevLoadJob = nextLoadJob;
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to apply the property remotely");
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to build the load job");
			}
		}
	}
	//
	public boolean isInterrupted() {
		return interrupted;
	}
	//
	@Override
	public final void run() {
		if(isParallel) {
			LOG.info(Markers.MSG, "Execute load jobs in parallel");
			for(int i = loadJobSeq.size() - 1; i >= 0; i --) {
				try {
					loadJobSeq.get(i).start();
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to start the distributed load job"
					);
				}
			}
			final ExecutorService chainWaitExecSvc = Executors.newFixedThreadPool(
				loadJobSeq.size(), new GroupThreadFactory("chainFinishAwait")
			);
			for(final LoadExecutor nextLoadJob : loadJobSeq) {
				chainWaitExecSvc.submit(new AwaitAndCloseLoadJobTask(nextLoadJob, timeOut, timeUnit));
			}
			chainWaitExecSvc.shutdown();
			try {
				if(chainWaitExecSvc.awaitTermination(timeOut, timeUnit)) {
					LOG.info(Markers.MSG, "Load jobs are finished in time");
				} else {
					LOG.info(Markers.MSG, "Load jobs timeout, closing");
				}
			} catch(final InterruptedException e) {
				Thread.currentThread().interrupt(); // ???
			} finally {
				LOG.debug(
					Markers.MSG, "{} load jobs are not finished in time",
					chainWaitExecSvc.shutdownNow().size()
				);
				for(final LoadExecutor nextLoadJob : loadJobSeq) {
					try {
						nextLoadJob.interrupt();
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to interrupt the load job \"{}\"", nextLoadJob
						);
					}
				}
				for(final LoadExecutor nextLoadJob : loadJobSeq) {
					try {
						nextLoadJob.close();
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to close the load job \"{}\"", nextLoadJob
						);
					}
				}
			}
		} else {
			LOG.info(Markers.MSG, "Execute load jobs sequentially");
			for(final LoadExecutor nextLoadJob : loadJobSeq) {
				if(!interrupted) {
					// start
					try {
						nextLoadJob.start();
						LOG.debug(Markers.MSG, "Started the next load job: \"{}\"", nextLoadJob);
					} catch(final RemoteException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to start the load job \"{}\"", nextLoadJob
						);
					}
					// wait
					try {
						nextLoadJob.await(timeOut, timeUnit);
					} catch(final InterruptedException e) {
						LOG.debug(Markers.MSG, "{}: interrupted", nextLoadJob);
						interrupted = true;
						Thread.currentThread().interrupt();
					} catch(final RemoteException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to await the remote load job \"{}\"",
							nextLoadJob
						);
					}
				}
				//
				try {
					final ItemSrc itemSrc = nextLoadJob.getItemSrc();
					if(itemSrc instanceof DataItemFileSrc) {
						((DataItemFileSrc) itemSrc).delete();
					}
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to delete source items file");
				}
				//
				try {
					nextLoadJob.close();
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to close the load job \"{}\"", nextLoadJob
					);
				}
			}
		}
		//
		loadJobSeq.clear();
	}
	//
	public static void main(final String... args) {
		RunTimeConfig.initContext();
		final RunTimeConfig runTimeConfig = RunTimeConfig.getContext();
		//
		LOG.info(Markers.MSG, runTimeConfig);
		//
		try {
			final Chain chainScenario = new Chain(runTimeConfig);
			chainScenario.run();
			LOG.info(Markers.MSG, "Scenario end");
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			LogUtil.exception(LOG, Level.ERROR, e, "Scenario failed");
		}
	}
}
