package com.emc.mongoose.storage.mock.api;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.Future;
/**
 Created by kurila on 31.07.15.
 */
public interface ObjectContainerMock<T extends DataObjectMock>
extends Closeable {
	//
	String DEFAULT_NAME = RunTimeConfig.getContext().getRunName();
	//
	String getName();
	//
	int size();
	//
	Future<T> put(final String oid, final T obj)
	throws InterruptedException;
	//
	Future<T> get(final String oid)
	throws InterruptedException;
	//
	Future<T> remove(final String oid)
	throws InterruptedException;
	//
	Future<T> list(final String afterOid, final Collection<T> buffDst, final int limit)
	throws InterruptedException;
}
