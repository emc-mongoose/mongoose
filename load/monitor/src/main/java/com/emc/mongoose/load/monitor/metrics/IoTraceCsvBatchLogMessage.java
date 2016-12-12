package com.emc.mongoose.load.monitor.metrics;

import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.ui.log.LogMessageBase;

import java.util.List;

/**
 Created by andrey on 17.11.16.
 */
public final class IoTraceCsvBatchLogMessage<R extends IoResult>
extends LogMessageBase {

	private final List<R> ioResults;
	private final int from;
	private final int to;

	public IoTraceCsvBatchLogMessage(final List<R> ioResults, final int from, final int to) {
		this.ioResults = ioResults;
		this.from = from;
		this.to = to;
	}

	@Override @SuppressWarnings("unchecked")
	public final void formatTo(final StringBuilder strb) {
		if(to > from) {
			final R anyIoResult = ioResults.get(0);
			String nextItemInfo;
			int commaPos;
			if(anyIoResult instanceof DataIoResult) {
				final List<DataIoResult>
					dataIoResults = (List<DataIoResult>) ioResults;
				DataIoResult nextDataIoResult;
				for(int i = from; i < to; i ++) {
					nextDataIoResult = dataIoResults.get(i);
					nextItemInfo = nextDataIoResult.getItemInfo();
					if(nextItemInfo != null) {
						commaPos = nextItemInfo.indexOf(',');
						if(commaPos > 0) {
							nextItemInfo = nextItemInfo.substring(0, commaPos);
						}
					}
					IoTraceCsvLogMessage.format(
						strb,
						nextDataIoResult.getStorageDriverAddr(),
						nextDataIoResult.getStorageNodeAddr(),
						nextItemInfo,
						nextDataIoResult.getIoTypeCode(),
						nextDataIoResult.getStatusCode(),
						nextDataIoResult.getTimeStart(),
						nextDataIoResult.getDuration(),
						nextDataIoResult.getLatency(),
						nextDataIoResult.getDataLatency(),
						nextDataIoResult.getCountBytesDone()
					);
					if(i < to - 1) {
						strb.append('\n');
					}
				}
			} else {
				R nextIoResult;
				for(int i = from; i < to; i ++) {
					nextIoResult = ioResults.get(i);
					nextItemInfo = nextIoResult.getItemInfo();
					if(nextItemInfo != null) {
						commaPos = nextItemInfo.indexOf(',');
						if(commaPos > 0) {
							nextItemInfo = nextItemInfo.substring(0, commaPos);
						}
					}
					IoTraceCsvLogMessage.format(
						strb,
						nextIoResult.getStorageDriverAddr(),
						nextIoResult.getStorageNodeAddr(),
						nextItemInfo,
						nextIoResult.getIoTypeCode(),
						nextIoResult.getStatusCode(),
						nextIoResult.getTimeStart(),
						nextIoResult.getDuration(),
						nextIoResult.getLatency(),
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
