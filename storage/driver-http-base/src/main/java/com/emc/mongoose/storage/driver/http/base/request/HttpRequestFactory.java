package com.emc.mongoose.storage.driver.http.base.request;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import io.netty.handler.codec.http.HttpRequest;

import java.net.URISyntaxException;
/**
 Created by andrey on 22.09.16.
 */
public interface HttpRequestFactory<I extends Item, O extends IoTask<I>> {

	HttpRequest getHttpRequest(final O ioTask, final String nodeAddr)
	throws URISyntaxException;
}
