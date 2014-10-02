package com.emc.mongoose.base.api;
//
import com.emc.mongoose.base.data.DataItem;
//
import java.io.Closeable;
import java.util.concurrent.Callable;
//
/**
 Created by kurila on 02.06.14.
 Request entity supporting some common operations.
 */
public interface Request<T extends DataItem>
extends Callable<Request<T>>, Closeable {
	//
	T getDataItem();
	//
	Request<T> setDataItem(final T dataItem);
	//
	Request<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
	int getStatusCode();
	//
	long getStartNanoTime();
	//
	long getDuration();
	//
	enum Type { CREATE, READ, DELETE, UPDATE, APPEND }
	//
}
