package com.emc.mongoose.base.api;
//
import com.emc.mongoose.base.data.DataItem;
//
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
//
/**
 Created by kurila on 02.06.14.
 Request entity supporting some common operations.
 */
public interface Request<T extends DataItem>
extends Callable<Request<T>>, Closeable {
	//
	enum Type {
		CREATE, READ, DELETE, UPDATE, APPEND
	}
	//
	enum Result {
		SUCC,
		FAIL_CLIENT, FAIL_SVC, FAIL_NOT_FOUND, FAIL_AUTH, FAIL_CORRUPT,
		FAIL_IO, FAIL_TIMEOUT, FAIL_UNKNOWN
	}
	//
	T getDataItem();
	//
	Request<T> setDataItem(final T dataItem);
	//
	Request<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
	Result getResult();
	//
	long getStartTime();
	//
	long getDuration();
	//
	void execute()
	throws Exception;
}
