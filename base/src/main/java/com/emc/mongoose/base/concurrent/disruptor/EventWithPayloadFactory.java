package com.emc.mongoose.base.concurrent.disruptor;

import com.lmax.disruptor.EventFactory;

public final class EventWithPayloadFactory<T>
				implements EventFactory<EventWithPayload<T>> {

	@Override
	public final EventWithPayload<T> newInstance() {
		return new EventImpl<>();
	}
}
