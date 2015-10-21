package com.emc.mongoose.run.scenario;
//
import com.emc.mongoose.client.impl.load.builder.BasicWSContainerLoadBuilderClient;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.load.builder.BasicWSContainerLoadBuilder;
import com.emc.mongoose.util.builder.LoadBuilderFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 09.06.15.
 */
public final class Single
implements Runnable {
	//
	private final static Logger LOG;
	static {
		LogUtil.init();
		LOG = LogManager.getLogger();
	}
	//
	private final LoadExecutor loadJob;
	private final long timeOut;
	private final TimeUnit timeUnit;
	//
	public Single(final RunTimeConfig rtConfig) {
		final LoadBuilder loadBuilder = new BasicWSContainerLoadBuilder(rtConfig);
		final IOTask.Type loadType = IOTask.Type.valueOf(
			rtConfig.getString(RunTimeConfig.KEY_SCENARIO_SINGLE_LOAD).toUpperCase()
		);
		//
		try {
			LOG.debug(Markers.MSG, "Using load type: {}", loadType.name());
			loadBuilder.setLoadType(loadType);
		} catch (final RemoteException e) {
			LogUtil.exception(LOG, Level.FATAL, e,
				"Failed to set load type for \"{}\"", loadBuilder);
		}
		//
		final long timeOut = rtConfig.getLoadLimitTimeValue();
		//
		this.timeOut = timeOut > 0 ? timeOut : Long.MAX_VALUE;
		this.timeUnit = timeOut > 0 ? rtConfig.getLoadLimitTimeUnit() : TimeUnit.DAYS;
		try {
			this.loadJob = loadBuilder.build();
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.FATAL, e, "Failed to build the load job");
			throw new IllegalStateException(e);
		}
	}
	//
	@Override
	public final void run() {
		try {
			loadJob.start();
			loadJob.await(timeOut, timeUnit);
		} catch(final RemoteException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to invoke a remote method for the load job: {}", loadJob
			);
		} catch(final InterruptedException e) {
			LOG.debug(Markers.MSG, "Load job \"{}\" was interrupted", loadJob);
		} finally {
			try {
				loadJob.close();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to close the load job: {}", loadJob);
			}
		}
	}
	//
	public static void main(final String... args) {
		//
		try {
			RunTimeConfig.initContext();
			final RunTimeConfig runTimeConfig = RunTimeConfig.getContext();
			//
			LOG.info(Markers.MSG, runTimeConfig);
			//
			final Single singleLoadScenario = new Single(runTimeConfig);
			singleLoadScenario.run();
			LOG.info(Markers.MSG, "Scenario end");
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Scenario failed");
		}
	}
}
