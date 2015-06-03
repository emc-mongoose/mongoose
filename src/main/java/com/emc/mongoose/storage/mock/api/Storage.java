package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.storage.mock.api.stats.IOStats;
/**
 Created by kurila on 03.06.15.
 */
public interface Storage<T extends DataObject>
extends Runnable {
	//
	T get(final String id);
	long getSize();
	long getCapacity();
	IOStats getStats();
	//
	void create(final T dataItem);
	void delete(final T dataItem);
}
