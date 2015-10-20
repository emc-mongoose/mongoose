package com.emc.mongoose.core.api.io.req;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.io.task.IOTask;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import java.io.Closeable;
import java.io.Externalizable;
/**
 Created by kurila on 29.09.14.
 Shared request configuration.
 */
public interface RequestConfig<T extends Item>
extends Externalizable, Cloneable, Closeable {
	//
	long serialVersionUID = 42L;
	int REQUEST_NO_PAYLOAD_TIMEOUT_SEC = 100,
		REQUEST_WITH_PAYLOAD_TIMEOUT_SEC = 100000;
	//
	String
		HOST_PORT_SEP = ":",
		PACKAGE_IMPL_BASE = "com.emc.mongoose.storage.adapter";
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
	String getNameSpace();
	RequestConfig<T> setNameSpace(final String nameSpace);
	//
	String getNamePrefix();
	RequestConfig<T> setNamePrefix(final String namePrefix);
	//
	ContentSource getContentSource();
	RequestConfig<T> setContentSource(final ContentSource dataSrc);
	//
	boolean getVerifyContentFlag();
	RequestConfig<T> setVerifyContentFlag(final boolean verifyContentFlag);
	//
	int getBuffSize();
	RequestConfig<T> setBuffSize(final int buffSize);
	//
	RequestConfig<T> setProperties(final RunTimeConfig props);
	//
	ItemSrc<T> getContainerListInput(final long maxCount, final String addr);
	//
	Class<T> getDataItemClass();
	//
	void configureStorage(final String storageAddrs[])
	throws IllegalStateException;
	//
	boolean isClosed();
}
