package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
/**
 Created by kurila on 29.09.14.
 A request configuration regarding data objects.
 */
public interface DataObjectRequestConfig<T extends DataObject>
extends RequestConfig<T> {
	//
	String REL_PKG_PROVIDERS = "provider";
	//
	@Override
	DataObjectRequestConfig<T> setAPI(final String api);
	//
	@Override
	DataObjectRequestConfig<T> setAddr(final String addr);
	//
	@Override
	DataObjectRequestConfig<T> setLoadType(final Request.Type loadType);
	//
	@Override
	DataObjectRequestConfig<T> setPort(final int port);
	//
	@Override
	DataObjectRequestConfig<T> setUserName(final String userName);
	//
	@Override
	DataObjectRequestConfig<T> setSecret(final String secret);
	//
	@Override
	DataObjectRequestConfig<T> setDataSource(final DataSource<T> dataSrc);
	//
	@Override
	DataObjectRequestConfig<T> setRetries(final boolean retryFlag);
	//
	@Override
	DataObjectRequestConfig<T> setProperties(final RunTimeConfig props);
	//
	@Override @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
	DataObjectRequestConfig<T> clone();
	//
}
