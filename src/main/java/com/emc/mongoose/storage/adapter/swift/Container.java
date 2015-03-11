package com.emc.mongoose.storage.adapter.swift;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.data.DataObject;
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
