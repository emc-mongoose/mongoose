package com.emc.mongoose.storage.driver.net.http.swift;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.storage.driver.net.http.base.HttpResponseHandlerBase;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;

import io.netty.handler.codec.http.HttpHeaders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 Created by andrey on 26.11.16.
 */
public final class SwiftResponseHandler<I extends Item, O extends IoTask<I>>
extends HttpResponseHandlerBase<I, O> {

	private static final Logger LOG = LogManager.getLogger();

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
