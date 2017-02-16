package com.emc.mongoose.storage.driver.net.http.atmos;

import com.emc.mongoose.model.io.task.IoTask;

import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.net.http.base.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;

/**
 Created by kurila on 11.11.16.
 */
public final class AtmosResponseHandler<I extends Item, O extends IoTask<I>>
extends HttpResponseHandlerBase<I, O> {
	
	private final boolean fsAccess;
	
	public AtmosResponseHandler(
		final HttpStorageDriverBase<I, O> driver, final boolean verifyFlag,
		final boolean fsAccess
	) {
		super(driver, verifyFlag);
		this.fsAccess = fsAccess;
	}
	
	@Override
	protected final void handleResponseHeaders(final O ioTask, final HttpHeaders respHeaders) {
		if(!fsAccess) {
			final String location = respHeaders.get(HttpHeaderNames.LOCATION);
			if(location != null && !location.isEmpty()) {
				ioTask.getItem().setName(location);
				// set the paths to null to avoid the path calculation in the ioTaskCallback call
				ioTask.setSrcPath(null);
				ioTask.setDstPath(null);
			}
		}
	}
}
