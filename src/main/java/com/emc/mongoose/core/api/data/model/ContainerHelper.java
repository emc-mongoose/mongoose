package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
//
import java.lang.reflect.Constructor;
/**
 Created by kurila on 03.07.15.
 */
public interface ContainerHelper<T extends DataItem, C extends Container<T>> {
	//
	int DEFAULT_PAGE_SIZE = 0x1000;
	//
	boolean exists(final String addr)
	throws IllegalStateException;
	//
	void create(final String addr)
	throws IllegalStateException;
	//
	void delete(final String addr)
	throws IllegalStateException;
	//
	void setVersioning(final String addr, final boolean enabledFlag)
	throws IllegalStateException;
	//
	T buildItem(final Constructor<T> itemConstructor, String rawId, final long size)
	throws IllegalStateException;
	//
}
