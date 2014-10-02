package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.data.WSDataObject;
//
import org.apache.http.client.ResponseHandler;
/**
 Created by kurila on 29.09.14.
 A HTTP request for performing an operation on data object.
 */
public interface WSObjectRequest<T extends WSDataObject>
extends DataObjectRequest<T>, ResponseHandler<WSObjectRequest<T>> {
	//
	@Override
	WSObjectRequest<T> setDataItem(final T dataItem);
	//
	@Override
	WSObjectRequest<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
}
