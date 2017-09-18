package com.emc.mongoose.scenario.step;

import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 The composite step implementation which executes the child steps simultaneously.
 */
public class ParallelStep
extends StepBase
implements CompositeStep {

	private final List<Step> children;

	public ParallelStep(final Config config) {
		this(config, null, null);
	}

	protected ParallelStep(
		final Config config, final Map<String, Object> stepConfig, final List<Step> children
	) {
		super(config, stepConfig);
		this.children = children;
	}

	@Override
	protected ParallelStep copyInstance(final Map<String, Object> stepConfig) {
		return new ParallelStep(baseConfig, stepConfig, children);
	}

	@Override
	protected String getTypeName() {
		return "parallel";
	}

	@Override
	public StepBase config(final Map<String, Object> stepConfig)
	throws ScenarioParseException {
		throw new ScenarioParseException(
			getTypeName() + " step type shouldn't contain the \"config\" section"
		);
	}

	@Override
	public ParallelStep steps(final Map<String, Object> children)
	throws ScenarioParseException {
		final List<Step> steps;
		if(children == null) {
			steps = null;
		} else {
			steps = new ArrayList<>(children.size());
			for(final Object child : children.values()) {
				if(child instanceof Step) {
					steps.add(((Step) child));
				} else {
					throw new ScenarioParseException();
				}
			}
		}
		return new ParallelStep(baseConfig, stepConfig, steps);
	}

	@Override
	protected void invoke(final Config config)
	throws Throwable {

		if(children == null) {
			return;
		}

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
