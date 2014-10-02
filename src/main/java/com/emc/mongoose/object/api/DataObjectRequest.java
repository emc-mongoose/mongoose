package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 29.09.14.
 A request regarding a data object.
 */
public interface DataObjectRequest<T extends DataObject>
extends Request<T> {
	//
	@Override
	DataObjectRequest<T> setDataItem(final T dataItem);
	//
	@Override
	DataObjectRequest<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
}
