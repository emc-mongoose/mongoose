package com.emc.mongoose.storage.driver.net.http.atmos;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.net.http.base.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import com.emc.mongoose.ui.log.Markers;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosApi.NS_URI_BASE;
import static com.emc.mongoose.storage.driver.net.http.atmos.AtmosApi.OBJ_URI_BASE;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 Created by kurila on 11.11.16.
 */
public final class AtmosResponseHandler<I extends Item, O extends IoTask<I>>
extends HttpResponseHandlerBase<I, O> {
	
	private static final Logger LOG = LogManager.getLogger();
	
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
					ioTask.getItem().setName(location.substring(NS_URI_BASE.length()));
				} else if(location.startsWith(OBJ_URI_BASE)) {
					ioTask.getItem().setName(location.substring(OBJ_URI_BASE.length()));
				} else {
					ioTask.getItem().setName(location);
					LOG.warn(Markers.ERR, "Unexpected location value: \"{}\"", location);
				}
				// set the paths to null to avoid the path calculation in the ioTaskCallback call
				ioTask.setSrcPath(null);
				ioTask.setDstPath(null);
			}
		}
	}
}
