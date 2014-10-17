package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.data.WSObject;
/**
 Created by kurila on 29.09.14.
 A HTTP request for performing an operation on data object.
 */
public interface WSRequest<T extends WSObject>
extends DataObjectRequest<T> {
	//
	@Override
	WSRequest<T> setDataItem(final T dataItem);
	//
	@Override
	WSRequest<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
}
