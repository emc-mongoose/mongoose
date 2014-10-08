package com.emc.mongoose.object.api.provider.atmos;
//
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
	boolean exists()
	throws IllegalStateException;
	//
	void create()
	throws IllegalStateException;
	//
	void delete()
	throws IllegalStateException;
	//
	List<T> list()
	throws IllegalStateException;
}
