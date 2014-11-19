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
		SUCC(0, "Success"),
		FAIL_CLIENT(1, "Client failure/invalid request"),
		FAIL_SVC(2, "Storage failure"),
		FAIL_NOT_FOUND(3, "Item not found"),
		FAIL_AUTH(4, "Authentication/access failure"),
		FAIL_CORRUPT(5, "Data item corruption"),
		FAIL_IO(6, "I/O failure"),
		FAIL_TIMEOUT(7, "Timeout"),
		FAIL_UNKNOWN(8, "Unknown failure");
		public final int code;
		public final String description;
		Result(final int code, final String description) {
			this.code = code;
			this.description = description;
		}
	}
	//
	Request<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
	T getDataItem();
	//
	Request<T> setDataItem(final T dataItem);
	//
	long getTransferSize();
	//
	Result getResult();
	//
	long getStartTime();
	//
	long getDuration();
	//
	long getLatency();
	//
	void execute()
	throws Exception;
}
