package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.Closeable;
/**
 Created by kurila on 03.06.15.
 */
public interface StorageMock<T extends DataItem>
extends Runnable, Closeable {
	//
	long getSize();
	long getCapacity();
	IOStats getStats();
	//
	void putIntoDefaultContainer(final T dataItem);
}
