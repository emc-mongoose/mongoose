package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 29.09.14.
 A request regarding a data object.
 */
public interface DataObjectIOTask<T extends DataObject>
extends AsyncIOTask<T> {
	//
	@Override
	DataObjectIOTask<T> setDataItem(final T dataItem);
	//
	@Override
	DataObjectIOTask<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
	@Override
	DataObjectIOTask<T> setNodeAddr(final String nodeAddr);
}
