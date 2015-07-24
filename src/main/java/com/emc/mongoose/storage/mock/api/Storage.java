package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import java.io.Closeable;
/**
 Created by kurila on 03.06.15.
 */
public interface Storage<T extends DataItem>
extends Runnable, Closeable {
	//
	long getSize();
	long getCapacity();
	IOStats getStats();
	// async methods
	void create(final T dataItem);
	void delete(final T dataItem);
	void update(final T dataItem, long start, long end);
	void append(final T dataItem, long start, long len);
}
