package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.io.Externalizable;

/**
 Created by kurila on 11.07.16.
 */
public interface IoTask<I extends Item>
extends Externalizable {

	String SLASH = "/";

	enum Status {
		PENDING,
		ACTIVE,
		CANCELLED,
		FAIL_UNKNOWN,
		SUCC,
		FAIL_IO,
		FAIL_TIMEOUT,
		RESP_FAIL_UNKNOWN,
		RESP_FAIL_CLIENT,
		RESP_FAIL_SVC,
		RESP_FAIL_NOT_FOUND,
		RESP_FAIL_AUTH,
		RESP_FAIL_CORRUPT,
		RESP_FAIL_SPACE
	}
	IoType getIoType();

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

	long getReqTimeStart();

	long getReqTimeDone();

	long getRespTimeStart();

	long getRespTimeDone();

	void reset();
}
