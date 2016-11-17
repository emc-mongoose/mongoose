package com.emc.mongoose.model.io.task.result;

/**
 Created by andrey on 17.11.16.
 */
public interface DataIoResult
extends IoResult {

	long getDataLatency();

	long getCountBytesDone();
}
