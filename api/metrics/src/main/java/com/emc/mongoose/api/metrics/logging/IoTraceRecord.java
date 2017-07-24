package com.emc.mongoose.api.metrics.logging;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.Item;

/**
 Created by andrey on 24.07.17.
 */
public final class IoTraceRecord<I extends Item, O extends IoTask<I>> {

	protected final String storageNode;
	protected final String itemPath;
	protected final int ioTypeCode;
	protected final int statusCode;
	protected final long reqTimeStart;
	protected final long duration;
	protected final long respLatency;
	protected final long dataLatency;
	protected final long transferSize;

	public IoTraceRecord(final O ioTaskResult) {
		storageNode = ioTaskResult.getNodeAddr();
		final String itemInfo = ioTaskResult.getItem().toString();
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
		ioTypeCode = ioTaskResult.getIoType().ordinal();
		statusCode = ioTaskResult.getStatus().ordinal();
		reqTimeStart = ioTaskResult.getReqTimeStart();
		duration = ioTaskResult.getRespTimeDone() - reqTimeStart;
		long t = ioTaskResult.getRespTimeStart() - ioTaskResult.getReqTimeDone();
		respLatency = t < duration && t > 0 ? t : -1;
		if(ioTaskResult instanceof DataIoTask) {
			final DataIoTask dataIoResult = (DataIoTask) ioTaskResult;
			t = ioTaskResult.getReqTimeDone() - dataIoResult.getRespDataTimeStart();
			dataLatency = t < duration && t > 0 ? t : -1;
			transferSize = dataIoResult.getCountBytesDone();
		} else {
			dataLatency = -1;
			transferSize = -1;
		}
	}

	public final void format(final StringBuilder strb) {
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
