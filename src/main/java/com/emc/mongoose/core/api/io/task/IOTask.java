package com.emc.mongoose.core.api.io.task;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
// mongoose-common.jar
import com.emc.mongoose.common.collections.Reusable;
/**
 Created by kurila on 02.06.14.
 Request entity supporting some common operations.
 */
public interface IOTask<T extends DataItem>
extends Reusable<IOTask<T>> {
	//
	enum Type {
		CREATE, READ, DELETE, UPDATE, APPEND
	}
	//
	String
		FMT_PERF_TRACE = "%s,%s,%d,%x,%d,%d,%d",
		FMT_PERF_TRACE_INVALID = "Invalid trace: %s,%s,%d,%x,%d,%d,%d,%d";
	//
	enum Status {
		SUCC(0, "Success"),
		FAIL_CLIENT(1, "Client failure/invalid request"),
		FAIL_SVC(2, "Storage failure"),
		FAIL_NOT_FOUND(3, "Item not found"),
		FAIL_AUTH(4, "Authentication/access failure"),
		FAIL_CORRUPT(5, "Data item corruption"),
		FAIL_IO(6, "I/O failure"),
		FAIL_TIMEOUT(7, "Timeout"),
		FAIL_UNKNOWN(8, "Unknown failure"),
		FAIL_NO_SPACE(9, "Not enough space on the storage");
		public final int code;
		public final String description;
		Status(final int code, final String description) {
			this.code = code;
			this.description = description;
		}
	}
	//
	IOTask<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
	IOTask<T> setNodeAddr(final String nodeAddr)
	throws IllegalStateException;
	//
	String getNodeAddr();
	//
	IOTask<T> setDataItem(final T dataItem);
	T getDataItem();
	//
	long getTransferSize();
	//
	Status getStatus();
	//
	int getLatency();
	//
	long getReqTimeStart();
	//
	long getReqTimeDone();
	//
	long getRespTimeStart();
	//
	long getRespTimeDone();
	//
	void complete();
}
