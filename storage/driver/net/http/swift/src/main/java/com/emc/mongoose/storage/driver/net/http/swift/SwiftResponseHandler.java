package com.emc.mongoose.storage.driver.net.http.swift;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.storage.driver.net.http.base.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;

import io.netty.handler.codec.http.HttpHeaders;

/**
 Created by andrey on 26.11.16.
 */
public final class SwiftResponseHandler<I extends Item, O extends IoTask<I>>
extends HttpResponseHandlerBase<I, O> {

	public SwiftResponseHandler(
		final HttpStorageDriverBase<I, O> driver, final boolean verifyFlag
	) {
		super(driver, verifyFlag);
	}

	@Override
	protected final void handleResponseHeaders(
		final O ioTask, final HttpHeaders respHeaders
	) {
	}
}
