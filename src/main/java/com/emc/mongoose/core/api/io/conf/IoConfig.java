package com.emc.mongoose.core.api.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.ContentSource;
//
import java.io.Closeable;
import java.io.Externalizable;
/**
 Created by kurila on 23.11.15.
 */
public interface IoConfig<T extends Item, C extends Container<T>>
extends Externalizable, Cloneable, Closeable {
	//
	long serialVersionUID = 42L;
	//
	IoConfig<T, C> clone()
	throws CloneNotSupportedException;
	//
	boolean isClosed();
	//
	LoadType getLoadType();
	IoConfig<T, C> setLoadType(final LoadType loadType);
	//
	String getNameSpace();
	IoConfig<T, C> setNameSpace(final String nameSpace);
	//
	String getNamePrefix();
	IoConfig<T, C> setNamePrefix(final String namePrefix);
	//
	int getNameRadix();
	IoConfig<T, C> setNameRadix(final int radix);
	//
	ContentSource getContentSource();
	IoConfig<T, C> setContentSource(final ContentSource dataSrc);
	//
	boolean getVerifyContentFlag();
	IoConfig<T, C> setVerifyContentFlag(final boolean verifyContentFlag);
	//
	int getBuffSize();
	IoConfig<T, C> setBuffSize(final int buffSize);
	//
	C getContainer();
	IoConfig<T, C> setContainer(final C container);
	//
	IoConfig<T, C> setAppConfig(final AppConfig appConfig);
	//
	Input<T> getContainerListInput(final long maxCount, final String addr);
	//
	Class<C> getContainerClass();
	//
	Class<T> getItemClass();
}
