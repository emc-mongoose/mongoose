package com.emc.mongoose.base.logging;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;

/** Created by andrey on 17.11.16. */
@AsynchronouslyFormattable
public final class OperationTraceCsvBatchLogMessage<I extends Item, O extends Operation<I>>
				extends LogMessageBase {

	private final List<OperationTraceRecord<I, O>> opTraceRecords;
	private final int size;

	public OperationTraceCsvBatchLogMessage(final List<O> opsResults, final int from, final int to) {
		size = to - from;
		opTraceRecords = new ArrayList<>(size);
		for (int i = from; i < to; i++) {
			opTraceRecords.add(new OperationTraceRecord<>(opsResults.get(i)));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void formatTo(final StringBuilder strb) {
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				opTraceRecords.get(i).format(strb);
			}
		}
	}
}
