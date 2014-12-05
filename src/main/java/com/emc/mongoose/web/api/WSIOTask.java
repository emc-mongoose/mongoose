package com.emc.mongoose.web.api;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.api.DataObjectIOTask;
import com.emc.mongoose.web.api.impl.ReusableHTTPRequest;
import com.emc.mongoose.web.data.WSObject;
//
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
/**
 Created by kurila on 29.09.14.
 A HTTP request for performing an operation on data object.
 */
public interface WSIOTask<T extends WSObject>
extends DataObjectIOTask<T>, HttpAsyncRequestProducer, HttpAsyncResponseConsumer<AsyncIOTask.Result> {
	//
	enum HTTPMethod {
		//
		DELETE, GET, HEAD, PUT, POST, TRACE;
		//
		public MutableHTTPRequest createRequest() {
			return new ReusableHTTPRequest(this, null, "/");
		}
	}
	//
	@Override
	WSIOTask<T> setDataItem(final T dataItem);
	//
	@Override
	WSIOTask<T> setRequestConfig(final RequestConfig<T> reqConf);
}
