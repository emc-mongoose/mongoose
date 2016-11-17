package com.emc.mongoose.model.io.task.result;

/**
 Created by andrey on 17.11.16.
 */
public class BasicDataIoResult
extends BasicIoResult
implements DataIoResult {

	private final long dataLatency;
	private final long transferredByteCount;

	public BasicDataIoResult(
		final String storageDriverAddr, final String storageNodeAddr, final String itemInfo,
		final int ioTypeCode, final int statusCode, final long reqTimeStart,
		final long duration, final long latency, final long dataLatency,
		final long transferredByteCount
	) {
		super(
			storageDriverAddr, storageNodeAddr, itemInfo, ioTypeCode, statusCode, reqTimeStart,
			duration, latency
		);
		this.dataLatency = dataLatency;
		this.transferredByteCount = transferredByteCount;
	}

	@Override
	public final long getDataLatency() {
		return dataLatency;
	}

	@Override
	public final long getCountBytesDone() {
		return transferredByteCount;
	}
}
