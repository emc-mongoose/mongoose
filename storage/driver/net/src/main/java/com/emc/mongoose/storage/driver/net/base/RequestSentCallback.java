package com.emc.mongoose.storage.driver.net.base;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.ui.log.LogUtil;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import org.apache.logging.log4j.Level;

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
		try {
			ioTask.finishRequest();
		} catch(final IllegalStateException e) {
			LogUtil.exception(Level.DEBUG, e, "{}", ioTask.toString());
		}
	}
}
