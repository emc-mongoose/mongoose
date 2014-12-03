package com.emc.mongoose.web.api;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.api.DataObjectRequest;
import com.emc.mongoose.web.data.WSObject;
//
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
/**
 Created by kurila on 29.09.14.
 A HTTP request for performing an operation on data object.
 */
public interface WSRequest<T extends WSObject>
extends DataObjectRequest<T>, HttpAsyncRequestProducer, HttpAsyncResponseConsumer<Request.Result> {
	//
	enum HTTPMethod { DELETE, GET, HEAD, PUT, POST, TRACE }
	//
	@Override
	WSRequest<T> setDataItem(final T dataItem);
	//
	@Override
	WSRequest<T> setRequestConfig(final RequestConfig<T> reqConf);
}
