package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.util.builder.LoadBuilderFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 02.02.16.
 */
public final class SingleJobContainer
extends JobContainerBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadExecutor loadJob;
	private final long limitTime;
	//
	public SingleJobContainer(final AppConfig appConfig) {
		super(appConfig);
		limitTime = localConfig.getLoadLimitTime();
		LoadBuilder loadJobBuilder = null;
		try {
			loadJobBuilder = LoadBuilderFactory.getInstance(localConfig);
		} catch(final ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to get a load builder instance");
		} catch(final InvocationTargetException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e.getTargetException(), "Failed to get a load builder instance"
			);
		}
		LoadExecutor loadJob_ = null;
		try {
			loadJob_ = loadJobBuilder == null ? null : loadJobBuilder.build();
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to build the load job");
		} finally {
			loadJob = loadJob_;
		}
	}
	//
	public SingleJobContainer(final LoadExecutor loadJob, final long limitTime) {
		super(null);
		this.loadJob = loadJob;
		this.limitTime = limitTime;
	}
	//
	@Override
	public final boolean append(final JobContainer subJob) {
		throw new IllegalStateException("Appending sub jobs to a single load job is not allowed");
	}
	//
	public final LoadExecutor get() {
		return loadJob;
	}
	//
	@Override
	public final String toString() {
		try {
			return loadJob.getName();
		} catch(final RemoteException e) {
			throw new RuntimeException(e);
		}
	}
	//
	@Override
	public final void run() {
		try {
			LOG.info(Markers.MSG, "Start the job \"{}\"", loadJob.getName());
			loadJob.start();
			try {
				loadJob.await(
					limitTime > 0 ? limitTime : Long.MAX_VALUE,
					limitTime > 0 ? TimeUnit.SECONDS : TimeUnit.DAYS
				);
				try {
					loadJob.close();
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to close the load job {}", loadJob
					);
				}
			} catch(final InterruptedException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Load job {} was interrupted", loadJob);
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.WARN, e,
					"Failed to invoke the await method remotely for the load job {}", loadJob
				);
			}
		} catch(final RemoteException e) {
			LogUtil.exception(
				LOG, Level.WARN, e,
				"Failed to invoke the start method remotely for the load job {}", loadJob
			);
		}
	}
}
