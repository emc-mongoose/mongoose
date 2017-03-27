package com.emc.mongoose.run.scenario.step;

import com.emc.mongoose.ui.config.Config;

/**
 Created by andrey on 07.06.16.
 */
public final class BasicTaskStep
implements Step {

	private final Runnable task;

	public BasicTaskStep(final Runnable task) {
		this.task = task;
	}

	@Override
	public final Config getConfig() {
		return null;
	}

	@Override
	public final void close() {
	}

	@Override
	public final void run() {
		task.run();
	}
}
