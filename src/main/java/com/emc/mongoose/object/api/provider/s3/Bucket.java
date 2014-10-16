package com.emc.mongoose.object.api.provider.s3;
//
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 02.10.14.
 */
public interface Bucket<T extends DataObject> {
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
}
