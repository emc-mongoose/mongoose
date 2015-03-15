package com.emc.mongoose.core.api.io.req.conf;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.src.DataSource;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.impl.util.RunTimeConfig;
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
	String HOST_PORT_SEP = ":";
	//
	RequestConfig<T> clone()
	throws CloneNotSupportedException;
	//
	String getAPI();
	RequestConfig<T> setAPI(final String api);
	//
	IOTask.Type getLoadType();
	RequestConfig<T> setLoadType(final IOTask.Type loadType);
	//
	String getScheme();
	RequestConfig<T> setScheme(final String scheme);
	//
	//String getAddr();
	//WSRequestConfigImpl<T> setAddr(final String addr);
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
	IOTask<T> getRequestFor(final T dataItem, final String nodeAddr)
	throws InterruptedException;
	//
	RequestConfig<T> setAnyDataProducerEnabled(final boolean enabled);
	Producer<T> getAnyDataProducer(final long maxCount, final String addr);
	//
	void configureStorage(final String storageAddrs[])
	throws IllegalStateException;
	//
	boolean isClosed();
}
