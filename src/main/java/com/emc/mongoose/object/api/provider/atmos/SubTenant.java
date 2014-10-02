package com.emc.mongoose.object.api.provider.atmos;
//
import com.emc.mongoose.object.data.DataObject;
//
import java.util.List;
/**
 Created by kurila on 02.10.14.
 */
public interface SubTenant<T extends DataObject> {
	String getName();
	void create();
	void delete();
	List<T> enumerate();
}
