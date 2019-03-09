package com.emc.mongoose.storage.driver.coop.netty;

import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.logging.LogUtil;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import org.apache.logging.log4j.Level;

/**
Created by andrey on 15.12.16.
*/
public final class RequestSentCallback
				implements FutureListener<Void> {

	private final Operation op;

	public RequestSentCallback(final Operation op) {
		this.op = op;
	}

	@Override
	public final void operationComplete(final Future<Void> future)
					throws Exception {
		try {
			op.finishRequest();
		} catch (final IllegalStateException e) {
			LogUtil.exception(Level.DEBUG, e, "{}", op.toString());
		}
	}
}
