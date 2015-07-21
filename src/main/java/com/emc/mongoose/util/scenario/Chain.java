package com.emc.mongoose.util.scenario;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.load.tasks.AwaitLoadJobTask;
//
import com.emc.mongoose.run.cli.HumanFriendly;
import com.emc.mongoose.util.scenario.shared.WSLoadBuilderFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
	private final boolean isConcurrent;
	//
	public Chain(
		final LoadBuilder loadBuilder, final long timeOut, final TimeUnit timeUnit,
		final String[] loadTypesSeq, final boolean isConcurrent
	) {
		this.timeOut = timeOut > 0 ? timeOut : Long.MAX_VALUE;
		this.timeUnit = timeOut > 0 ? timeUnit : TimeUnit.DAYS;
		this.isConcurrent = isConcurrent;
		//
		if(!isConcurrent) {
			try {
				loadBuilder.getRequestConfig().setAnyDataProducerEnabled(false);
			} catch(final RemoteException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to disable *any* data producer");
			}
		}
		//
		LoadExecutor prevLoadJob = null, nextLoadJob;
		for(final String loadTypeStr : loadTypesSeq) {
			LOG.debug(Markers.MSG, "Next load type is \"{}\"", loadTypeStr);
			try {
				loadBuilder.setLoadType(IOTask.Type.valueOf(loadTypeStr.toUpperCase()));
				nextLoadJob = loadBuilder.build();
				if(prevLoadJob == null) {
					// prevent the file list producer creation for next load jobs
					loadBuilder.setInputFile(null);
				} else {
					prevLoadJob.setConsumer(nextLoadJob);
				}
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
	@Override
	public final void run() {
		if(isConcurrent) {
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
				chainWaitExecSvc.submit(new AwaitLoadJobTask(nextLoadJob, timeOut, timeUnit));
			}
			chainWaitExecSvc.shutdown();
			try {
				if(chainWaitExecSvc.awaitTermination(timeOut, timeUnit)) {
					LOG.debug(Markers.MSG, "Load jobs are finished in time");
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
			boolean interrupted = false;
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
		//
		RunTimeConfig.initContext();
		final RunTimeConfig runTimeConfig = RunTimeConfig.getContext();
		// load the config from CLI arguments
		final Map<String, String> properties = HumanFriendly.parseCli(args);
		if(properties != null && !properties.isEmpty()) {
			LOG.debug(Markers.MSG, "Overriding properties {}", properties);
			RunTimeConfig.getContext().overrideSystemProperties(properties);
		}
		//
		LOG.info(Markers.MSG, RunTimeConfig.getContext().toString());
		//
		try(final LoadBuilder loadBuilder = WSLoadBuilderFactory.getInstance(runTimeConfig)) {
			final long timeOut = runTimeConfig.getLoadLimitTimeValue();
			final TimeUnit timeUnit = runTimeConfig.getLoadLimitTimeUnit();
			//
			final String[] loadTypeSeq = runTimeConfig.getScenarioChainLoad();
			final boolean isConcurrent = runTimeConfig.getScenarioChainConcurrentFlag();
			//
			final Chain chainScenario = new Chain(
				loadBuilder, timeOut, timeUnit, loadTypeSeq, isConcurrent
			);
			chainScenario.run();
		} catch(final Exception e) {
			e.printStackTrace(System.err);
			LogUtil.exception(LOG, Level.ERROR, e, "Scenario failed");
		}
	}
}
