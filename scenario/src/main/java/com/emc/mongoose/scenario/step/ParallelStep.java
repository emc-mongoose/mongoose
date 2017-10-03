package com.emc.mongoose.scenario.step;

import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.scenario.ScenarioParseException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.Loggers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
		this(config, null);
	}

	protected ParallelStep(final Config config, final List<Step> children) {
		super(config);
		this.children = children;
	}

	@Override
	protected String getTypeName() {
		return "parallel";
	}

	@Override
	public ParallelStep step(final Step child)
	throws ScenarioParseException {
		final List<Step> childrenCopy = new ArrayList<>();
		if(children != null) {
			childrenCopy.addAll(children);
		}
		if(child != null) {
			childrenCopy.add(child);
		}
		return new ParallelStep(baseConfig, childrenCopy);
	}

	@Override
	protected void invoke(final Config config)
	throws Throwable {

		if(children == null || children.isEmpty()) {
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

	@Override
	public void close()
	throws IOException {
		if(children != null) {
			children.clear();
		}
	}
}
