package com.emc.mongoose.object.api.impl.provider.swift;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 03.03.15.
 */
public interface AuthToken<T extends DataObject> {
	String getValue();
	//
	//boolean exists(final LoadExecutor<T> client)
	//throws IllegalStateException;
	//
	void create(final LoadExecutor<T> client)
	throws IllegalStateException;
	//
	//void delete(final LoadExecutor<T> client)
	//throws IllegalStateException;
}
