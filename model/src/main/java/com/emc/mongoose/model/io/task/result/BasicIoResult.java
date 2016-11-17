package com.emc.mongoose.model.io.task.result;

/**
 Created by andrey on 17.11.16.
 */
public class BasicIoResult
implements IoResult {

	private final String storageDriverAddr;
	private final String storageNodeAddr;
	private final String itemInfo;
	private final int ioTypeCode;
	private final int statusCode;
	private final long reqTimeStart;
	private final long duration;
	private final long latency;

	public BasicIoResult(
		final String storageDriverAddr, final String storageNodeAddr, final String itemInfo,
		final int ioTypeCode, final int statusCode, final long reqTimeStart,
		final long duration, final long latency
	) {
		this.storageDriverAddr = storageDriverAddr;
		this.storageNodeAddr = storageNodeAddr;
		this.itemInfo = itemInfo;
		this.ioTypeCode = ioTypeCode;
		this.statusCode = statusCode;
		this.reqTimeStart = reqTimeStart;
		this.duration = duration;
		this.latency = latency;
	}

	@Override
	public final String getStorageDriverAddr() {
		return storageDriverAddr;
	}

	@Override
	public final String getStorageNodeAddr() {
		return storageNodeAddr;
	}

	@Override
	public final String getItemInfo() {
		return itemInfo;
	}

	@Override
	public final int getIoTypeCode() {
		return ioTypeCode;
	}

	@Override
	public final int getStatusCode() {
		return statusCode;
	}

	@Override
	public final long getTimeStart() {
		return reqTimeStart;
	}

	@Override
	public final long getDuration() {
		return duration;
	}

	@Override
	public final long getLatency() {
		return latency;
	}
}
