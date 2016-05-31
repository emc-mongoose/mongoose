package com.emc.mongoose.core.api.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.conf.enums.LoadType;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.ContentSource;
//
import java.io.Closeable;
import java.io.Externalizable;
import java.io.IOException;

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
	String getItemNamingPrefix();
	IoConfig<T, C> setItemNamingPrefix(final String namingPrefix);
	//
	int getItemNamingLength();
	IoConfig<T, C> setItemNamingLength(final int namingLength);
	//
	int getItemNamingRadix();
	IoConfig<T, C> setItemNamingRadix(final int namingRadix);
	//
	long getItemNamingOffset();
	IoConfig<T, C> setItemNamingOffset(final long namingOffset);
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
	C getDstContainer();
	IoConfig<T, C> setDstContainer(final C container);
	//
	C getSrcContainer();
	IoConfig<T, C> setSrcContainer(final C container);
	//
	IoConfig<T, C> setAppConfig(final AppConfig appConfig);
	//
	Input<T> getNewDataItemsInput(
		final ItemNamingType namingType, final Class<T> itemClass, final SizeInBytes sizeInfo
	) throws NoSuchMethodException;
	//
	Input<C> getNewContainersInput(final ItemNamingType namingType, final Class<C> itemClass)
	throws NoSuchMethodException;
	//
	Input<T> getContainerListInput(final long maxCount, final String addr);
	//
	Class<C> getContainerClass();
	//
	Class<T> getItemClass();
	//
	String getItemPath();
}
