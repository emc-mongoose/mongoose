package com.emc.mongoose.model.io.task;

import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.load.LoadType;

import java.io.Externalizable;
import java.io.Serializable;

/**
 Created by kurila on 11.07.16.
 */
public interface IoTask<I extends Item>
extends Externalizable {

	String SLASH = "/";
	String STORAGE_DRIVER_ADDR = ServiceUtil.getHostAddr();

	enum Status {

		PENDING(0, "Pending"),
		ACTIVE(1, "Active"),
		CANCELLED(2, "Cancelled"),
		FAIL_UNKNOWN(3, "Unknown failure"),
		SUCC(4, "Success"),
		FAIL_IO(5, "I/O failure"),
		FAIL_TIMEOUT(6, "Timeout"),
		RESP_FAIL_UNKNOWN(7, "Unrecognized storage response"),
		RESP_FAIL_CLIENT(8, "Client failure/invalid request"),
		RESP_FAIL_SVC(9, "Storage failure"),
		RESP_FAIL_NOT_FOUND(10, "Item not found"),
		RESP_FAIL_AUTH(11, "Authentication/access failure"),
		RESP_FAIL_CORRUPT(12, "Data item corruption"),
		RESP_FAIL_SPACE(13, "Not enough space on the storage");

		public final int code;

		public final String description;

		Status(final int code, final String description) {
			this.code = code;
			this.description = description;
		}
	}

	interface IoResult
	extends Serializable {

		LoadType getLoadType();

		Status getStatus();

		String getStorageDriverAddr();

		String getStorageNodeAddr();

		String getItemInfo();

		long getTimeStart();

		long getDuration();

		long getLatency();
	}

	LoadType getLoadType();

	I getItem();

	String getNodeAddr();

	void setNodeAddr(final String nodeAddr);

	Status getStatus();

	void setStatus(final Status status);

	String getSrcPath();
	
	void setSrcPath(final String srcPath);
	
	String getDstPath();
	
	void setDstPath(final String dstPath);

	void startRequest();

	void finishRequest();

	void startResponse();

	void finishResponse();

	<R extends IoResult> R getIoResult();

	void reset();
}
