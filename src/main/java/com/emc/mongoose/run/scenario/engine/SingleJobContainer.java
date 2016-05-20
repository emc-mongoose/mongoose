package com.emc.mongoose.run.scenario.engine;
//

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
import java.util.concurrent.TimeUnit;

//
//
//
//
/**
 Created by kurila on 02.02.16.
 */
public final class SingleJobContainer
extends JobContainerBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadBuilder loadJobBuilder;
	private final LoadExecutor loadJob;
	//
	public SingleJobContainer(final AppConfig appConfig) {
		super(appConfig);
		try {
			loadJobBuilder = LoadBuilderFactory.getInstance(localConfig);
		} catch(
			final ClassNotFoundException | NoSuchMethodException | InstantiationException |
				IllegalAccessException e
		) {
			throw new IllegalStateException("Failed to get a load builder instance", e);
		} catch(final InvocationTargetException e) {
			throw new IllegalStateException(
				"Failed to get a load builder instance", e.getTargetException()
			);
		}
		loadJob = null;
	}
	//
	public SingleJobContainer(final LoadExecutor loadJob, final long limitTime) {
		super(null, limitTime);
		this.loadJobBuilder = null;
		this.loadJob = loadJob;
	}
	//
	@Override
	public final boolean append(final JobContainer subJob) {
		throw new IllegalStateException("Appending sub jobs to a single load job is not allowed");
	}
	//
	@Override
	public final String toString() {
		try {
			return loadJob == null ?
				loadJobBuilder == null ?
					null :
					loadJobBuilder.toString() :
				loadJob.getName();
		} catch(final RemoteException e) {
			throw new RuntimeException(e);
		}
	}
	//
	@Override
	public final void run() {
		//
		LoadExecutor loadJob_ = null;
		//
		if(loadJob == null) {
			if(loadJobBuilder == null) {
				LOG.error(Markers.ERR, "Both load job and builder are null");
			} else {
				try {
					loadJob_ = loadJobBuilder.build();
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to build the load job");
				}
			}
		} else {
			loadJob_ = loadJob;
		}
		if(loadJob_ == null) {
			return;
		}
		//
		try {
			LOG.info(Markers.MSG, "Start the job \"{}\"", loadJob_.getName());
			loadJob_.start();
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to start the load job");
		}
		//
		try {
			loadJob_.await(
				limitTime > 0 ? limitTime : Long.MAX_VALUE,
				limitTime > 0 ? TimeUnit.SECONDS : TimeUnit.DAYS
			);
		} catch(final InterruptedException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Load job {} was interrupted", loadJob_);
		} catch(final RemoteException e) {
			LogUtil.exception(
				LOG, Level.WARN, e,
				"Failed to invoke the await method remotely for the load job {}", loadJob_
			);
		} finally {
			try {
				loadJob_.close();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e,
					"Failed to invoke the start method remotely for the load job {}", loadJob_
				);
			}
		}
	}
}
