package com.emc.mongoose.model.api.io.task;

import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.util.LoadType;

/**
 Created by kurila on 11.07.16.
 */
public interface IoTask<I extends Item> {

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
	
	LoadType getLoadType();

	I getItem();

	String getNodeAddr();
	
	void setNodeAddr(final String nodeAddr);

	Status getStatus();

	void setStatus(final Status status);

	long getReqTimeStart();
	
	void setReqTimeStart(final long reqTimeStart);
	
	void setReqTimeDone(final long reqTimeDone);
	
	void setRespTimeStart(final long respTimeStart);
	
	void setRespTimeDone(final long respTimeDone);

	int getDuration();

	int getLatency();

	String getDstPath();
	
	void reset();
}
