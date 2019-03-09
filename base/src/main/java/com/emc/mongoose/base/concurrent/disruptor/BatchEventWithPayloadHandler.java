package com.emc.mongoose.base.concurrent.disruptor;

import com.lmax.disruptor.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class BatchEventWithPayloadHandler<T>
				implements EventHandler<EventWithPayload<T>> {

	private final Consumer<List<T>> batchHandler;
	private volatile List<T> payloadBuff = new ArrayList<>();

	public BatchEventWithPayloadHandler(final Consumer<List<T>> batchHandler) {
		this.batchHandler = batchHandler;
	}

	@Override
	public void onEvent(final EventWithPayload<T> evt, final long seq, final boolean eob)
					throws Exception {
		payloadBuff.add(evt.payload());
		if (eob) {
			batchHandler.accept(payloadBuff);
			payloadBuff = new ArrayList<>();
		}
	}
}
