package com.emc.mongoose.core.api.io.conf;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import java.io.Closeable;
/**
 Created by kurila on 29.09.14.
 Shared request configuration.
 */
public interface RequestConfig<T extends DataItem, C extends Container<T>>
extends IOConfig<T, C>, Closeable {
	//
	int REQUEST_NO_PAYLOAD_TIMEOUT_SEC = 100,
		REQUEST_WITH_PAYLOAD_TIMEOUT_SEC = 100000;
	//
	String HOST_PORT_SEP = ":";
	//
	@Override
	RequestConfig<T, C> clone()
	throws CloneNotSupportedException;
	//
	int getPort();
	RequestConfig<T, C> setPort(final int port);
	//
	@Override
	RequestConfig<T, C> setRunTimeConfig(final RunTimeConfig props);
	//
	void configureStorage(final String storageAddrs[])
	throws IllegalStateException;
}
