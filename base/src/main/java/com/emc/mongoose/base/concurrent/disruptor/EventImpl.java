package com.emc.mongoose.base.concurrent.disruptor;

public final class EventImpl<T>
				implements EventWithPayload<T> {

	private volatile T payload;

	public EventImpl() {}

	public final void payload(final T payload) {
		this.payload = payload;
	}

	public final T payload() {
		return payload;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "(" + payload + ')';
	}
}
