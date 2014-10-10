package com.emc.mongoose.base.api;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import java.io.Externalizable;
/**
 Created by kurila on 29.09.14.
 Shared request configuration.
 */
public interface RequestConfig<T extends DataItem>
extends Externalizable {
	//
	long serialVersionUID = 42L;
	//
	String getAPI();
	RequestConfig<T> setAPI(final String api);
	//
	String getAddr();
	RequestConfig<T> setAddr(final String addr);
	//
	Request.Type getLoadType();
	RequestConfig<T> setLoadType(final Request.Type loadType);
	//
	int getPort();
	RequestConfig<T> setPort(final int port);
	//
	String getUserName();
	RequestConfig<T> setUserName(final String userName);
	//
	String getSecret();
	RequestConfig<T> setSecret(final String secret);
	//
	DataSource<T> getDataSource();
	RequestConfig<T> setDataSource(final DataSource<T> dataSrc);
	//
	boolean getRetries();
	RequestConfig<T> setRetries(final boolean retryFlag);
	//
	RequestConfig<T> setProperties(final RunTimeConfig props);
	//
	RequestConfig<T> clone()
	throws CloneNotSupportedException;
	//
	void configureStorage()
	throws IllegalStateException;
}
