package com.emc.mongoose.model.io.task.result;

import java.io.Serializable;

/**
 Created by andrey on 17.11.16.
 */
public interface IoResult
extends Serializable {

	String getStorageDriverAddr();

	String getStorageNodeAddr();

	String getItemInfo();

	int getIoTypeCode();

	int getStatusCode();

	long getTimeStart();

	long getDuration();

	long getLatency();
}
