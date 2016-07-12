package com.emc.mongoose.common.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 Created on 12.07.16.
 */
public abstract class LifeCycleBase
implements LifeCycle {

	private AtomicReference<State> state;

	public enum State {
		STARTED, SHUTDOWN, INTERRUPTED
	}

	@Override
	public void start()
	throws IllegalStateException {

	}

	@Override
	public void shutdown()
	throws IllegalStateException {
	}

	@Override
	public void interrupt()
	throws IllegalStateException {
	}
}
