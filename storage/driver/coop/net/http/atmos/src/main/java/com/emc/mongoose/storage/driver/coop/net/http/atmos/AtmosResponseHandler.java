package com.emc.mongoose.storage.driver.coop.net.http.atmos;

import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.storage.driver.coop.net.http.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.coop.net.http.HttpStorageDriverBase;

import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.NS_URI_BASE;
import static com.emc.mongoose.storage.driver.coop.net.http.atmos.AtmosApi.OBJ_URI_BASE;

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
				if(location.startsWith(NS_URI_BASE)) {
					ioTask.item().setName(location.substring(NS_URI_BASE.length()));
				} else if(location.startsWith(OBJ_URI_BASE)) {
					ioTask.item().setName(location.substring(OBJ_URI_BASE.length()));
				} else {
					ioTask.item().setName(location);
					Loggers.ERR.warn("Unexpected location value: \"{}\"", location);
				}
				// set the paths to null to avoid the path calculation in the ioTaskCompleted call
				ioTask.srcPath(null);
				ioTask.dstPath(null);
			}
		}
	}
}
