package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.ui.config.Config;

import java.util.concurrent.TimeUnit;

public final class LocalStepSlice
implements StepSlice {

	private final transient Step parent;
	private final Config configSlice;


	@Override
	public State state()
	throws Exception {
		return null;
	}

	@Override
	public LocalStepSlice start()
	throws IllegalStateException, Exception {
		return null;
	}

	@Override
	public LocalStepSlice stop()
	throws IllegalStateException, Exception {
		return null;
	}

	@Override
	public LocalStepSlice await()
	throws InterruptedException, Exception {
		return null;
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, Exception {
		return false;
	}

	@Override
	public void close()
	throws Exception {
	}

	@Override
	public void run() {
	}
}
