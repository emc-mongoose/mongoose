package com.emc.mongoose.object.api.provider.atmos;
//
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.data.DataObject;
//
import java.util.List;
/**
 Created by kurila on 02.10.14.
 */
public interface SubTenant<T extends DataObject> {
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
}
