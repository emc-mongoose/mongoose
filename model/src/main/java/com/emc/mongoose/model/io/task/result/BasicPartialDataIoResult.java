package com.emc.mongoose.model.io.task.result;

/**
 Created by andrey on 23.11.16.
 */
public class BasicPartialDataIoResult
extends BasicDataIoResult
implements PartialDataIoResult {

	public BasicPartialDataIoResult(
		final String storageDriverAddr, final String storageNodeAddr, final String itemInfo,
		final int ioTypeCode, final int statusCode, final long reqTimeStart, final long duration,
		final long latency, final long dataLatency, final long transferredByteCount
	) {
		super(
			storageDriverAddr, storageNodeAddr, itemInfo, ioTypeCode, statusCode, reqTimeStart,
			duration, latency, dataLatency, transferredByteCount
		);
	}
}
