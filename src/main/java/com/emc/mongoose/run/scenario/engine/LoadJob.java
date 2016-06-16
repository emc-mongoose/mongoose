package com.emc.mongoose.run.scenario.engine;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.util.builder.LoadBuilderFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 02.02.16.
 */
public final class LoadJob
extends JobBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public LoadJob(
		final AppConfig appConfig, final Map<String, Object> subTree, final boolean preconditionFlag
	) {
		super(appConfig);
		final Map<String, Object> nodeConfig = (Map<String, Object>) subTree.get(KEY_NODE_CONFIG);
		if(nodeConfig != null) {
			localConfig.override(null, nodeConfig);
		}
		localConfig.setProperty(AppConfig.KEY_LOAD_METRICS_PRECONDITION, preconditionFlag);
	}
	//
	@Override
	public final void run() {
		super.run();
		final long limitTime = localConfig.getLoadLimitTime();
		try(final LoadBuilder loadJobBuilder = LoadBuilderFactory.getInstance(localConfig)) {
			try(final LoadExecutor loadJob = loadJobBuilder.build()) {
				try {
					LOG.info(Markers.MSG, "Start the job \"{}\"", loadJob.getName());
					loadJob.start();
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to start the load job");
				}
				try {
					loadJob.await(
						limitTime > 0 ? limitTime : Long.MAX_VALUE,
						limitTime > 0 ? TimeUnit.SECONDS : TimeUnit.DAYS
					);
				} catch(final InterruptedException e) {
					LogUtil.exception(
						LOG, Level.DEBUG, e, "Load job {} was interrupted", loadJob.getName()
					);
				} catch(final RemoteException e) {
					LogUtil.exception(
						LOG, Level.WARN, e,
						"Failed to invoke the await method remotely for the load job {}",
						loadJob.getName()
					);
				}
			}
		} catch(
			final ClassNotFoundException | NoSuchMethodException | InstantiationException |
				IllegalAccessException e
		) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to get a load builder instance");
		} catch(final InvocationTargetException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e.getTargetException(), "Failed to get a load builder instance"
			);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to build a load job");
		} catch(final Throwable e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
		}
	}
	//
	@Override
	public final String toString() {
		return "singleLoadJobContainer#" + hashCode();
	}
	//
	@Override
	public final void close() {
	}
}
