package com.emc.mongoose.core.api.v1.io.task;
//
import com.emc.mongoose.core.api.v1.item.base.Item;
//
import org.apache.http.protocol.HttpContext;
//
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
/**
 Created by kurila on 29.09.14.
 A HTTP request for performing an operation on data object.
 */
public interface HttpIoTask<T extends Item, K extends HttpIoTask<T, K>>
extends IoTask<T>,
	HttpAsyncRequestProducer,
	HttpAsyncResponseConsumer<K>,
	HttpContext {
}
