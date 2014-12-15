package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
/**
 Created by kurila on 29.09.14.
 A request configuration regarding data objects.
 */
public interface ObjectRequestConfig<T extends DataObject>
extends RequestConfig<T> {
	//
	String REL_PKG_PROVIDERS = "provider";
	//
	@Override
	ObjectRequestConfig<T> setAPI(final String api);
	//
	@Override
	ObjectRequestConfig<T> setLoadType(final AsyncIOTask.Type loadType);
	//
	@Override
	ObjectRequestConfig<T> setUserName(final String userName);
	//
	@Override
	ObjectRequestConfig<T> setSecret(final String secret);
	//
	@Override
	ObjectRequestConfig<T> setDataSource(final DataSource<T> dataSrc);
	//
	@Override
	ObjectRequestConfig<T> setRetries(final boolean retryFlag);
	//
	@Override
	ObjectRequestConfig<T> setProperties(final RunTimeConfig props);
}
