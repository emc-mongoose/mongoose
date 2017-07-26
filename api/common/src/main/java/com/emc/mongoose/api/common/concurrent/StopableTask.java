package com.emc.mongoose.api.common.concurrent;

import java.io.Closeable;

/**
 Created by andrey on 19.04.17.
 */
public interface StopableTask
extends Closeable, Runnable {

	int TIMEOUT_MILLIS = 250;

	boolean isClosed();
}
