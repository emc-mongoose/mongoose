package com.emc.mongoose.base.api;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.util.pool.BasicInstancePool;
//
import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 02.06.14.
 Request entity supporting some common operations.
 */
public interface AsyncIOTask<T extends DataItem>
extends Closeable {
	//
	enum Type {
		CREATE, READ, DELETE, UPDATE, APPEND
	}
	//
	String FMT_PERF_TRACE = "%s,%x,%x,%x,%x,%x,%x";
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
		FAIL_UNKNOWN(8, "Unknown failure"),
		FAIL_NO_SPACE(9, "Not enough space on the storage");
		public final int code;
		public final String description;
		Result(final int code, final String description) {
			this.code = code;
			this.description = description;
		}
	}
	//
	ConcurrentHashMap<RequestConfig, BasicInstancePool<AsyncIOTask>>
		POOL_MAP = new ConcurrentHashMap<>();
	//
	AsyncIOTask<T> setRequestConfig(final RequestConfig<T> reqConf);
	//
	AsyncIOTask<T> setNodeAddr(final String nodeAddr);
	String getNodeAddr();
	//
	AsyncIOTask<T> setDataItem(final T dataItem);
	T getDataItem();
	//
	long getTransferSize();
	//
	Result getResult();
	//
	long getReqTimeStart();
	//
	long getReqTimeDone();
	//
	long getRespTimeStart();
	//
	long getRespTimeDone();
	//
	void reset();
	//
	void complete();
}
