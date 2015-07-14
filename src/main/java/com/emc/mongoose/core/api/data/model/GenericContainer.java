package com.emc.mongoose.core.api.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;

import java.lang.reflect.Constructor;
/**
 Created by kurila on 03.07.15.
 */
public interface GenericContainer<T extends DataItem> {
	//
	String getName();
	//
	int getBatchSize();
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
	T buildItem(final Constructor<T> itemConstructor, String rawId, final long size)
	throws IllegalStateException;
	//
}
