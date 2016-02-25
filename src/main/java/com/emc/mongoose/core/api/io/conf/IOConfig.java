package com.emc.mongoose.core.api.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.base.ItemSrc;
//
import java.io.Closeable;
import java.io.Externalizable;
/**
 Created by kurila on 23.11.15.
 */
public interface IOConfig<T extends DataItem, C extends Container<T>>
extends Externalizable, Cloneable, Closeable {
	//
	long serialVersionUID = 42L;
	//
	IOConfig<T, C> clone()
	throws CloneNotSupportedException;
	//
	boolean isClosed();
	//
	AppConfig.LoadType getLoadType();
	IOConfig<T, C> setLoadType(final AppConfig.LoadType loadType);
	//
	String getNameSpace();
	IOConfig<T, C> setNameSpace(final String nameSpace);
	//
	String getNamePrefix();
	IOConfig<T, C> setNamePrefix(final String namePrefix);
	//
	int getNameRadix();
	IOConfig<T, C> setNameRadix(final int radix);
	//
	ContentSource getContentSource();
	IOConfig<T, C> setContentSource(final ContentSource dataSrc);
	//
	boolean getVerifyContentFlag();
	IOConfig<T, C> setVerifyContentFlag(final boolean verifyContentFlag);
	//
	int getBuffSize();
	IOConfig<T, C> setBuffSize(final int buffSize);
	//
	C getContainer();
	IOConfig<T, C> setContainer(final C container);
	//
	IOConfig<T, C> setAppConfig(final AppConfig appConfig);
	//
	ItemSrc<T> getContainerListInput(final long maxCount, final String addr);
	//
	Class<C> getContainerClass();
	//
	Class<T> getItemClass();
}
