package com.emc.mongoose.storage.driver.coop.netty.http.swift;

import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.coop.netty.http.HttpStorageDriverBase;

import io.netty.handler.codec.http.HttpHeaders;

/**
 Created by andrey on 26.11.16.
 */
public final class SwiftResponseHandler<I extends Item, O extends Operation<I>>
extends HttpResponseHandlerBase<I, O> {

	public SwiftResponseHandler(
		final HttpStorageDriverBase<I, O> driver, final boolean verifyFlag
	) {
		super(driver, verifyFlag);
	}

	@Override
	protected final void handleResponseHeaders(
		final O op, final HttpHeaders respHeaders
	) {
	}
}
