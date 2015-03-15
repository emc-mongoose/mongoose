package com.emc.mongoose.storage.adapter.swift;
//
import com.emc.mongoose.core.api.data.DataObject;
/**
 Created by kurila on 03.03.15.
 */
public interface AuthToken<T extends DataObject> {
	String getValue();
	//
	//boolean exists(final String addr)
	//throws IllegalStateException;
	//
	void create(final String addr)
	throws IllegalStateException;
	//
	//void delete(final String addr)
	//throws IllegalStateException;
}
