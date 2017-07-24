package com.emc.mongoose.api.metrics.logging;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.ui.log.LogMessageBase;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;

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
@AsynchronouslyFormattable
public final class IoTraceCsvLogMessage<I extends Item, O extends IoTask<I>>
extends LogMessageBase {

	private final IoTraceRecord<I, O> ioTraceRec;

	public IoTraceCsvLogMessage(final O ioTaskResult) {
		ioTraceRec = new IoTraceRecord<>(ioTaskResult);
	}

	@Override
	public final void formatTo(final StringBuilder strb) {
		ioTraceRec.format(strb);
	}
}
