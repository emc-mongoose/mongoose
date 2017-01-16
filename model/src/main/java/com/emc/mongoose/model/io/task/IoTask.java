package com.emc.mongoose.model.io.task;

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
		PENDING, // 0
		ACTIVE, // 1
		CANCELLED, // 2
		FAIL_UNKNOWN, // 3
		SUCC, // 4
		FAIL_IO, // 5
		FAIL_TIMEOUT, // 6
		RESP_FAIL_UNKNOWN, // 7
		RESP_FAIL_CLIENT, // 8
		RESP_FAIL_SVC, // 9
		RESP_FAIL_NOT_FOUND, // 10
		RESP_FAIL_AUTH, // 11
		RESP_FAIL_CORRUPT, // 12
		RESP_FAIL_SPACE // 13
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
	
	interface IoResult<I extends Item>
	extends Externalizable {
		
		String getStorageDriverAddr();
		
		String getStorageNodeAddr();

		@Deprecated
		String getItemInfo();
		
		I getItem();
		
		int getIoTypeCode();
		
		int getStatusCode();
		
		long getTimeStart();
		
		long getDuration();
		
		long getLatency();
	}

	default String buildItemInfo(final String itemPath, final String itemInfo) {
		if(itemPath == null || itemPath.isEmpty()) {
			if(itemInfo.startsWith("/")) {
				return itemInfo;
			} else {
				return "/" + itemInfo;
			}
		} else {
			if(itemPath.endsWith("/")) {
				return itemPath + itemInfo;
			} else {
				return itemPath + "/" + itemInfo;
			}
		}
	}
	
	R getResult(
		final String hostAddr,
		final boolean useStorageDriverResult,
		final boolean useStorageNodeResult,
		final boolean useItemInfoResult,
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
