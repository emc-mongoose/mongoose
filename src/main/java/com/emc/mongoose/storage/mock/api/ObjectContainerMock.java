package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;
/**
 Created by kurila on 31.07.15.
 */
public interface ObjectContainerMock<T extends DataObjectMock>
extends Closeable, Map<String, T> {
	//
	String DEFAULT_NAME = RunTimeConfig.getContext().getRunName();
	//
	String getName();
	//
	int size();
	//
	T list(final String afterOid, final Collection<T> buffDst, final int limit);
	//
	Future<T> submitPut(final T obj)
	throws InterruptedException;
	//
	Future<T> submitGet(final String oid)
	throws InterruptedException;
	//
	Future<T> submitRemove(final String oid)
	throws InterruptedException;
	//
	Future<T> submitList(final String afterOid, final Collection<T> buffDst, final int limit)
	throws InterruptedException;
}
