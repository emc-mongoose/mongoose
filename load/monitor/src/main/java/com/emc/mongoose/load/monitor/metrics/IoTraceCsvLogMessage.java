package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.ui.log.LogMessageBase;

/**
 Created by andrey on 17.11.16.

 StorageNode,
 ItemPath,
 IoTypeCode,
 StatusCode,
 ReqTimeStart[us],
 Duration[us],
 RespLatency[us],
 DataLatency[us],
 TransferSize
 */
public final class IoTraceCsvLogMessage<I extends Item, O extends IoTask<I>>
extends LogMessageBase {

	private final String storageNode;
	private final String itemPath;
	private final int ioTypeCode;
	private final int statusCode;
	private final long reqTimeStart;
	private final long duration;
	private final long respLatency;
	private final long dataLatency;
	private final long transferSize;

	public IoTraceCsvLogMessage(final O ioResult) {
		storageNode = ioResult.getNodeAddr();
		final String itemInfo = ioResult.getItem().toString();
		if(itemInfo != null) {
			final int commaPos = itemInfo.indexOf(',', 0);
			if(commaPos > 0) {
				itemPath = itemInfo.substring(0, itemInfo.indexOf(',', 0));
			} else {
				itemPath = itemInfo;
			}
		} else {
			itemPath = null;
		}
		ioTypeCode = ioResult.getIoType().ordinal();
		statusCode = ioResult.getStatus().ordinal();
		reqTimeStart = ioResult.getReqTimeStart();
		duration = ioResult.getRespTimeDone() - reqTimeStart;
		long t = ioResult.getRespTimeStart() - ioResult.getReqTimeDone();
		respLatency = t < duration && t > 0 ? t : -1;
		if(ioResult instanceof DataIoTask) {
			final DataIoTask dataIoResult = (DataIoTask) ioResult;
			t = ioResult.getReqTimeDone() - dataIoResult.getRespDataTimeStart();
			dataLatency = t < duration && t > 0 ? t : -1;
			transferSize = dataIoResult.getCountBytesDone();
		} else {
			dataLatency = -1;
			transferSize = -1;
		}
	}

	@Override
	public final void formatTo(final StringBuilder strb) {
		format(
			strb, storageNode, itemPath, ioTypeCode, statusCode, reqTimeStart, duration,
			respLatency, dataLatency, transferSize
		);
	}

	public static void format(
		final StringBuilder strb, final String storageNode, final String itemPath,
		final int ioTypeCode, final int statusCode, final long reqTimeStart, final long duration,
		final long respLatency, final long dataLatency, final long transferSize
	) {
		if(storageNode != null) {
			strb.append(storageNode);
		}
		strb.append(',');
		if(itemPath != null) {
			strb.append(itemPath);
		}
		strb.append(',');
		if(ioTypeCode != -1) {
			strb.append(ioTypeCode);
		}
		strb.append(',');
		if(statusCode != -1) {
			strb.append(statusCode);
		}
		strb.append(',');
		if(reqTimeStart > 0) {
			strb.append(reqTimeStart);
		}
		strb.append(',');
		if(duration > 0) {
			strb.append(duration);
		}
		strb.append(',');
		if(respLatency > 0) {
			strb.append(respLatency);
		}
		strb.append(',');
		if(dataLatency > 0) {
			strb.append(dataLatency);
		}
		strb.append(',');
		if(transferSize != -1) {
			strb.append(transferSize);
		}
		strb.append('\n');
	}
}
