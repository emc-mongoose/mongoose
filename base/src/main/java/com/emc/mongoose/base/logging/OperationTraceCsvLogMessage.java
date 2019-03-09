package com.emc.mongoose.base.logging;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;

/**
* Created by andrey on 17.11.16.
*
* <p>StorageNode, ItemPath, OpTypeCode, StatusCode, ReqTimeStart[us], Duration[us],
* RespLatency[us], DataLatency[us], TransferSize
*/
@AsynchronouslyFormattable
public final class OperationTraceCsvLogMessage<I extends Item, O extends Operation<I>>
				extends LogMessageBase {

	private final OperationTraceRecord<I, O> opTraceRec;

	public OperationTraceCsvLogMessage(final O opResult) {
		opTraceRec = new OperationTraceRecord<>(opResult);
	}

	@Override
	public final void formatTo(final StringBuilder strb) {
		opTraceRec.format(strb);
	}
}
