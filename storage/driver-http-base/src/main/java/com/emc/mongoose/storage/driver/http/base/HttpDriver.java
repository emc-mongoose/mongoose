package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.util.LoadType;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.AttributeKey;

import java.net.URISyntaxException;
/**
 Created by kurila on 30.08.16.
 */
public interface HttpDriver<I extends Item, O extends IoTask<I>>
extends Driver<I, O> {
	
	String SIGN_METHOD = "HmacSHA1";
	
	AttributeKey<IoTask> ATTR_KEY_IOTASK = AttributeKey.valueOf("ioTask");

	HttpMethod getHttpMethod(final LoadType loadType);

	String getDstUriPath(final I item, final O ioTask);

	void applyCopyHeaders(final HttpHeaders httpHeaders, final I obj)
	throws URISyntaxException;
}
