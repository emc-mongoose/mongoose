package com.emc.mongoose.model.io.task;

import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.io.Externalizable;

/**
 Created by kurila on 11.07.16.
 */
public interface IoTask<I extends Item, R extends IoTask.IoResult>
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
	
	interface IoResult
	extends Externalizable {
		
		String getStorageDriverAddr();
		
		String getStorageNodeAddr();
		
		String getItemInfo();
		
		int getIoTypeCode();
		
		int getStatusCode();
		
		long getTimeStart();
		
		long getDuration();
		
		long getLatency();
	}
	
	R getResult(
		final String hostAddr,
		final boolean useStorageDriverResult,
		final boolean useStorageNodeResult,
		final boolean useItemPathResult,
		final boolean useIoTypeCodeResult,
		final boolean useStatusCodeResult,
		final boolean useReqTimeStartResult,
		final boolean useDurationResult,
		final boolean useRespLatencyResult,
		final boolean useDataLatencyResult,
		final boolean useTransferSizeResult
	);

	void reset();
}
