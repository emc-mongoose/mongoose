package com.emc.mongoose.load.monitor.metrics;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.ui.log.LogMessageBase;

import java.util.List;

/**
 Created by andrey on 17.11.16.
 */
public final class IoTraceCsvBatchLogMessage<I extends Item, O extends IoTask<I>>
extends LogMessageBase {

	private final List<O> ioResults;
	private final int from;
	private final int to;

	public IoTraceCsvBatchLogMessage(final List<O> ioResults, final int from, final int to) {
		this.ioResults = ioResults;
		this.from = from;
		this.to = to;
	}

	@Override @SuppressWarnings("unchecked")
	public final void formatTo(final StringBuilder strb) {
		if(to > from) {
			final O anyIoResult = ioResults.get(0);
			String nextItemInfo;
			int commaPos;
			long nextReqTimeStart, nextReqTimeDone;
			if(anyIoResult instanceof DataIoTask) {
				final List<DataIoTask> dataIoResults = (List) ioResults;
				DataIoTask nextResult;
				for(int i = from; i < to; i ++) {
					nextResult = dataIoResults.get(i);
					nextItemInfo = nextResult.getItem().toString();
					if(nextItemInfo != null) {
						commaPos = nextItemInfo.indexOf(',', 0);
						if(commaPos > 0) {
							nextItemInfo = nextItemInfo.substring(0, commaPos);
						}
					}
					nextReqTimeStart = nextResult.getReqTimeStart();
					nextReqTimeDone = nextResult.getReqTimeDone();
					IoTraceCsvLogMessage.format(
						strb,
						nextResult.getNodeAddr(),
						nextItemInfo,
						nextResult.getIoType().ordinal(),
						nextResult.getStatus().ordinal(),
						nextReqTimeStart,
						nextResult.getRespTimeDone() - nextReqTimeStart,
						nextResult.getRespTimeStart() - nextReqTimeDone,
						nextResult.getRespDataTimeStart() - nextReqTimeDone,
						nextResult.getCountBytesDone()
					);
					if(i < to - 1) {
						strb.append('\n');
					}
				}
			} else {
				O nextResult;
				for(int i = from; i < to; i ++) {
					nextResult = ioResults.get(i);
					nextItemInfo = nextResult.getItem().toString();
					if(nextItemInfo != null) {
						commaPos = nextItemInfo.indexOf(',', 0);
						if(commaPos > 0) {
							nextItemInfo = nextItemInfo.substring(0, commaPos);
						}
					}
					nextReqTimeStart = nextResult.getReqTimeStart();
					IoTraceCsvLogMessage.format(
						strb,
						nextResult.getNodeAddr(),
						nextItemInfo,
						nextResult.getIoType().ordinal(),
						nextResult.getStatus().ordinal(),
						nextReqTimeStart,
						nextResult.getRespTimeDone() - nextReqTimeStart,
						nextResult.getRespTimeStart() - nextResult.getReqTimeDone(),
						-1,
						-1
					);
					if(i < to - 1) {
						strb.append('\n');
					}
				}
			}
		}
	}
}
