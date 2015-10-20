package com.emc.mongoose.core.api.io.task;
//
//
import com.emc.mongoose.core.api.data.WSObject;
	import org.apache.http.protocol.HttpContext;
//
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
/**
 Created by kurila on 29.09.14.
 A HTTP request for performing an operation on data object.
 */
public interface
	WSIOTask<T extends WSObject>
extends
	IOTask<T>,
	HttpAsyncRequestProducer,
	HttpAsyncResponseConsumer<WSIOTask<T>>,
	HttpContext {
}
