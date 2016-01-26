package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.item.base.Item;
//
import org.apache.http.protocol.HttpContext;
//
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
/**
 Created by kurila on 29.09.14.
 A HTTP request for performing an operation on data object.
 */
public interface
HttpIOTask<T extends Item, K extends HttpIOTask<T, K>>
extends
	IOTask<T>,
	HttpAsyncRequestProducer,
	HttpAsyncResponseConsumer<K>,
	HttpContext {
}
