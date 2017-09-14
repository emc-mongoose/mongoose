package com.emc.mongoose.run.scenario.jsr223;

import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import javax.script.Bindings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 The composite step implementation which executes the child steps simultaneously.
 */
public final class ParallelStep
extends StepBase
implements CompositeStep {

	private final List<Step> children;

	public ParallelStep(final Config config) {
		this(config, null);
	}

	private ParallelStep(final Config config, final Bindings children) {
		super(config);
		if(children == null) {
			this.children = null;
		} else {
			this.children = new ArrayList<>(children.size());
			for(final Object nextStep : children.values()) {
				if(nextStep instanceof Step) {
					this.children.add((Step) nextStep);
				} else {
					throw new AssertionError();
				}
			}
		}
	}

	@Override
	public final ParallelStep config(final Bindings stepConfig) {
		final Config configCopy = new Config(config);
		configCopy.apply(stepConfig, "parallel-" + LogUtil.getDateTimeStamp() + "-" + hashCode());
		return new ParallelStep(configCopy, stepConfig);
	}

	@Override
	public final ParallelStep include(final Bindings children) {
		return new ParallelStep(new Config(config), children);
	}

	@Override
	public final void run() {
		final ExecutorService parallelJobsExecutor = Executors.newFixedThreadPool(
			children.size(), new LogContextThreadFactory("stepWorker" + hashCode(), true)
		);
		for(final Step subStep : children) {
			parallelJobsExecutor.submit(subStep);
		}
		Loggers.MSG.info("{}: execute {} child steps in parallel", toString(), children.size());
		parallelJobsExecutor.shutdown();

		final long limitTime = config.getTestConfig().getStepConfig().getLimitConfig().getTime();
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
			"{}: finished parallel execution of {} child steps", toString(), children.size()
		);
	}
}
