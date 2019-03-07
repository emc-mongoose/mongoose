package com.emc.mongoose.storage.driver.coop.netty.http.atmos;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpStorageDriverBase;

import static com.emc.mongoose.storage.driver.coop.netty.http.atmos.AtmosApi.NS_URI_BASE;
import static com.emc.mongoose.storage.driver.coop.netty.http.atmos.AtmosApi.OBJ_URI_BASE;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;

/**
 Created by kurila on 11.11.16.
 */
public final class AtmosResponseHandler<I extends Item, O extends Operation<I>>
				extends HttpResponseHandlerBase<I, O> {

	private final boolean fsAccess;

	public AtmosResponseHandler(
					final HttpStorageDriverBase<I, O> driver, final boolean verifyFlag,
					final boolean fsAccess) {
		super(driver, verifyFlag);
		this.fsAccess = fsAccess;
	}

	@Override
	protected final void handleResponseHeaders(final Channel channel, final O op, final HttpHeaders respHeaders) {
		if (!fsAccess) {
			final String location = respHeaders.get(HttpHeaderNames.LOCATION);
			if (location != null && !location.isEmpty()) {
				if (location.startsWith(NS_URI_BASE)) {
					op.item().name(location.substring(NS_URI_BASE.length()));
				} else if (location.startsWith(OBJ_URI_BASE)) {
					op.item().name(location.substring(OBJ_URI_BASE.length()));
				} else {
					op.item().name(location);
					Loggers.ERR.warn("Unexpected location value: \"{}\"", location);
				}
				// set the paths to null to avoid the path calculation in the handleCompleted call
				op.srcPath(null);
				op.dstPath(null);
			}
		}
	}
}
