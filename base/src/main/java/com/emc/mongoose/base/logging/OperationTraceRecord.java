package com.emc.mongoose.base.logging;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.data.DataOperation;

/** Created by andrey on 24.07.17. */
public final class OperationTraceRecord<I extends Item, O extends Operation<I>> {

	protected final String storageNode;
	protected final String itemPath;
	protected final int opTypeCode;
	protected final int statusCode;
	protected final long reqTimeStart;
	protected final long duration;
	protected final long respLatency;
	protected final long dataLatency;
	protected final long transferSize;

	public OperationTraceRecord(final O opResult) {
		storageNode = opResult.nodeAddr();
		final String itemInfo = opResult.item().toString();
		if (itemInfo != null) {
			final int commaPos = itemInfo.indexOf(',', 0);
			if (commaPos > 0) {
				itemPath = itemInfo.substring(0, itemInfo.indexOf(',', 0));
			} else {
				itemPath = itemInfo;
			}
		} else {
			itemPath = null;
		}
		opTypeCode = opResult.type().ordinal();
		statusCode = opResult.status().ordinal();
		reqTimeStart = opResult.reqTimeStart();
		duration = opResult.respTimeDone() - reqTimeStart;
		long t = opResult.respTimeStart() - opResult.reqTimeDone();
		respLatency = t < duration && t > 0 ? t : -1;
		if (opResult instanceof DataOperation) {
			final DataOperation dataIoResult = (DataOperation) opResult;
			t = opResult.reqTimeDone() - dataIoResult.respDataTimeStart();
			dataLatency = t < duration && t > 0 ? t : -1;
			transferSize = dataIoResult.countBytesDone();
		} else {
			dataLatency = -1;
			transferSize = -1;
		}
	}

	public final void format(final StringBuilder strb) {
		if (storageNode != null) {
			strb.append(storageNode);
		}
		strb.append(',');
		if (itemPath != null) {
			strb.append(itemPath);
		}
		strb.append(',');
		if (opTypeCode != -1) {
			strb.append(opTypeCode);
		}
		strb.append(',');
		if (statusCode != -1) {
			strb.append(statusCode);
		}
		strb.append(',');
		if (reqTimeStart > 0) {
			strb.append(reqTimeStart);
		}
		strb.append(',');
		if (duration > 0) {
			strb.append(duration);
		}
		strb.append(',');
		if (respLatency > 0) {
			strb.append(respLatency);
		}
		strb.append(',');
		if (dataLatency > 0) {
			strb.append(dataLatency);
		}
		strb.append(',');
		if (transferSize != -1) {
			strb.append(transferSize);
		}
		strb.append('\n');
	}
}
