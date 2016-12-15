package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.io.task.IoTask;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

/**
 Created by andrey on 15.12.16.
 */
public final class RequestSentCallback
implements FutureListener<Void> {

	private final IoTask ioTask;

	public RequestSentCallback(final IoTask ioTask) {
		this.ioTask = ioTask;
	}

	@Override
	public final void operationComplete(final Future<Void> future)
	throws Exception {
		ioTask.finishRequest();
	}
}
