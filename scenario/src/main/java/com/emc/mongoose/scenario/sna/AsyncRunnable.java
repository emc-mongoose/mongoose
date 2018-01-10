package com.emc.mongoose.scenario.sna;

import java.util.concurrent.TimeUnit;

public interface AsyncRunnable
extends AutoCloseable, Runnable {

	enum State {
		INITIAL, STARTED, STOPPED, FINISHED
	}

	/**
	 @return the current state
	 */
	State state()
	throws Exception;

	/**
	 Start/resume the execution
	 @return the same instance with state changed to <i>STARTED</i> if call was successful.
	 @throws IllegalStateException if the previous state is not <i>INITIAL</i> neither <i>STOPPED</i>
	 */
	AsyncRunnable start()
	throws IllegalStateException, Exception;

	/**
	 Stop (with further resumption capability)
	 @return the same instance with state changed to <i>STOPPED</i> if call was successful
	 @throws IllegalStateException if the previous state is not <i>STARTED</i>
	 */
	AsyncRunnable stop()
	throws IllegalStateException, Exception;

	/**
	 Wait while the state is <i>STARTED</i>
	 @return the same instance
	 @throws InterruptedException
	 */
	AsyncRunnable await()
	throws InterruptedException, Exception;

	boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException, Exception;
}
