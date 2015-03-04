package com.emc.mongoose.object.api.impl.provider.swift;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 02.03.15.
 */
public interface Container<T extends DataObject> {
	//
	String getName();
	//
	boolean exists(final LoadExecutor<T> client)
	throws IllegalStateException;
	//
	void create(final LoadExecutor<T> client)
	throws IllegalStateException;
	//
	void delete(final LoadExecutor<T> client)
	throws IllegalStateException;
	//
}
