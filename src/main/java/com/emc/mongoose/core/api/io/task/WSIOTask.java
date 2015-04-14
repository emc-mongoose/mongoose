package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
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
	DataObjectIOTask<T>,
	HttpAsyncRequestProducer,
	HttpAsyncResponseConsumer<IOTask.Status> {
	//
	@Override
	WSIOTask<T> setDataItem(final T dataItem);
	//
	@Override
	WSIOTask<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
	@Override
	WSIOTask<T> setNodeAddr(final String nodeAddr)
	throws IllegalStateException;
}
