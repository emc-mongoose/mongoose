package com.emc.mongoose.object.api.provider.s3;
//
import com.emc.mongoose.object.data.DataObject;

import java.util.List;
/**
 Created by kurila on 02.10.14.
 */
public interface Bucket<T extends DataObject> {
	//
	String getName();
	//
	void create()
	throws IllegalStateException;
	//
	void delete()
	throws IllegalStateException;
	//
	List<T> list()
	throws IllegalStateException;
	//
}
