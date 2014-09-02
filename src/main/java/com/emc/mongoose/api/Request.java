package com.emc.mongoose.api;
//
import com.emc.mongoose.data.UniformData;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
//
/**
 Created by kurila on 02.06.14.
 */
public interface Request<T extends UniformData>
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
	enum Type { CREATE, READ, DELETE, UPDATE }
	//
}
