package com.emc.mongoose.base.api;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import java.io.Closeable;
import java.io.Externalizable;
/**
 Created by kurila on 29.09.14.
 Shared request configuration.
 */
public interface RequestConfig<T extends DataItem>
extends Externalizable, Cloneable, Closeable {
	//
	long serialVersionUID = 42L;
	//
	@SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
	RequestConfig<T> clone();
	//
	String getAPI();
	RequestConfig<T> setAPI(final String api);
	//
	String getAddr();
	RequestConfig<T> setAddr(final String addr);
	//
	AsyncIOTask.Type getLoadType();
	RequestConfig<T> setLoadType(final AsyncIOTask.Type loadType);
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
	boolean getVerifyContentFlag();
	RequestConfig<T> setVerifyContentFlag(final boolean verifyContentFlag);
	//
	RequestConfig<T> setProperties(final RunTimeConfig props);
	//
	int getLoadNumber();
	RequestConfig<T> setLoadNumber(final int loadNumber);
	//
	AsyncIOClient<T> getClient();
	RequestConfig<T> setClient(final AsyncIOClient<T> storageClient);
	//
	void configureStorage()
	throws IllegalStateException;
	//
	boolean isClosed();
}
