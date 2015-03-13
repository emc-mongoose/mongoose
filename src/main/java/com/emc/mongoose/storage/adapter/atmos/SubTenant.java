package com.emc.mongoose.storage.adapter.atmos;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.data.DataObject;
//
/**
 Created by kurila on 02.10.14.
 */
public interface SubTenant<T extends DataObject> {
	//
	String getValue();
	//
	boolean exists(final LoadExecutor<T> client)
	throws IllegalStateException;
	//
	void create(final LoadExecutor<T> client)
	throws IllegalStateException;
	//
	void delete(final LoadExecutor<T> client)
	throws IllegalStateException;
}
