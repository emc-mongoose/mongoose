package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.run.scenario.ScenarioParseException;
import com.emc.mongoose.api.model.concurrent.NamingThreadFactory;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 02.02.16.
 */
public class ParallelStep
extends CompositeStepBase {
	//
	protected ParallelStep(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig, subTree);
	}
	//
	@Override
	protected final synchronized void invoke()
	throws CancellationException {

		final ExecutorService parallelJobsExecutor = Executors.newFixedThreadPool(
			childSteps.size(), new NamingThreadFactory("stepWorker" + hashCode(), true)
		);
		for(final Step subStep : childSteps) {
			parallelJobsExecutor.submit(subStep);
		}
		Loggers.MSG.info("{}: execute {} child steps in parallel", toString(), childSteps.size());
		parallelJobsExecutor.shutdown();
		
		final long limitTime = localConfig
			.getTestConfig().getStepConfig().getLimitConfig().getTime();
		try {
			if(limitTime > 0) {
				parallelJobsExecutor.awaitTermination(limitTime, TimeUnit.SECONDS);
			} else {
				parallelJobsExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
			}
		} catch(final InterruptedException e) {
			Loggers.MSG.debug("{}: interrupted the child steps execution", toString());
			throw new CancellationException();
		} finally {
			parallelJobsExecutor.shutdownNow();
		}
		Loggers.MSG.info(
			"{}: finished parallel execution of {} child steps", toString(), childSteps.size()
		);
	}
	//
	@Override
	public String toString() {
		return "parallelStep#" + hashCode();
	}
}
