package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.api.impl.ReusableWSRequest;
import com.emc.mongoose.object.data.WSObject;
//
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
/**
 Created by kurila on 29.09.14.
 A HTTP request for performing an operation on data object.
 */
public interface
	WSIOTask<T extends WSObject>
extends
	DataObjectIOTask<T>,
	HttpAsyncRequestProducer,
	HttpAsyncResponseConsumer<AsyncIOTask.Status> {
	//
	enum HTTPMethod {
		//
		DELETE, GET, HEAD, PUT, POST, TRACE;
		//
		public MutableWSRequest createRequest() {
			return new ReusableWSRequest(this, null, "/");
		}
	}
	//
	@Override
	WSIOTask<T> setDataItem(final T dataItem);
	//
	@Override
	WSIOTask<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
	@Override
	WSIOTask<T> setNodeAddr(final String nodeAddr)
	throws InterruptedException;
	//
	HttpContext getHttpContext();
}
