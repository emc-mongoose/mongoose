package com.emc.mongoose.api.common.concurrent;

import java.io.Closeable;
import java.io.IOException;

/**
 Created by andrey on 19.04.17.
 */
public interface StoppableTask
extends Closeable, Runnable {

	/**
	 Stops the task
	 @throws IOException if some kind of failure occured
	 */
	@Override
	public void close()
	throws IOException;

	boolean isClosed();
}
