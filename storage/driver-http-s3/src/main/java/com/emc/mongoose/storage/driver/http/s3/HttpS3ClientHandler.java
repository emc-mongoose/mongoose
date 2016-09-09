package com.emc.mongoose.storage.driver.http.s3;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Monitor;
import com.emc.mongoose.storage.driver.http.base.HttpClientHandlerBase;
import io.netty.channel.ChannelHandler;

import java.util.concurrent.atomic.AtomicReference;

/**
 Created by kurila on 01.08.16.
 */
@ChannelHandler.Sharable
public final class HttpS3ClientHandler<I extends Item, O extends IoTask<I>>
extends HttpClientHandlerBase<I, O> {
	
	public HttpS3ClientHandler(final AtomicReference<Monitor<I, O>> monitorRef) {
		super(monitorRef);
	}
}
