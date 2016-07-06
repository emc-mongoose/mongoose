package com.emc.mongoose.run.scenario.engine;

import com.emc.mongoose.common.conf.AppConfig;

/**
 Created by andrey on 07.06.16.
 */
public final class BasicTaskJob
implements Job {

	private final Runnable task;

	public BasicTaskJob(final Runnable task) {
		this.task = task;
	}

	@Override
	public final AppConfig getConfig() {
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
