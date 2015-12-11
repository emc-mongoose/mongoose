package com.emc.mongoose.core.api.io.conf;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
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
	IOTask.Type getLoadType();
	IOConfig<T, C> setLoadType(final IOTask.Type loadType);
	//
	String getNameSpace();
	IOConfig<T, C> setNameSpace(final String nameSpace);
	//
	String getNamePrefix();
	IOConfig<T, C> setNamePrefix(final String namePrefix);
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
	IOConfig<T, C> setProperties(final RunTimeConfig props);
	//
	ItemSrc<T> getContainerListInput(final long maxCount, final String addr);
	//
	Class<C> getContainerClass();
	//
	Class<T> getItemClass();
}
