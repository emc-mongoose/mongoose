package com.emc.mongoose.api.metrics.logging;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.ui.log.LogMessageBase;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;

import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 17.11.16.
 */
@AsynchronouslyFormattable
public final class IoTraceCsvBatchLogMessage<I extends Item, O extends IoTask<I>>
extends LogMessageBase {

	private final List<IoTraceRecord<I, O>> ioTraceRecords;
	private final int size;

	public IoTraceCsvBatchLogMessage(final List<O> ioTaskResults, final int from, final int to) {
		size = to - from;
		if(size > 100_000) {
			Loggers.ERR.warn("I/O trace batch size too big: {}", to - from);
		}
		ioTraceRecords = new ArrayList<>(size);
		for(int i = from; i < to; i ++) {
			ioTraceRecords.add(new IoTraceRecord<>(ioTaskResults.get(i)));
		}
	}

	@Override @SuppressWarnings("unchecked")
	public final void formatTo(final StringBuilder strb) {
		if(size > 0) {
			for(int i = 0; i < size; i ++) {
				ioTraceRecords.get(i).format(strb);
			}
		}
	}
}
