package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.core.api.data.DataObject;

import java.util.concurrent.ExecutionException;
/**
 Created by kurila on 03.07.15.
 */
public interface ObjectStorageMock<T extends DataObject>
extends StorageMock<T> {
	//
	T create(final String id, final long offset, final long size)
	throws InterruptedException, ExecutionException;
	//
	T update(final String id, final long offset, final long size)
	throws InterruptedException, ExecutionException;
	//
	T append(final String id, final long offset, final long size)
	throws InterruptedException, ExecutionException;
	//
	T read(final String id, final long offset, final long size)
	throws InterruptedException, ExecutionException;
	//
	T delete(final String id)
	throws InterruptedException, ExecutionException;
	//
}
