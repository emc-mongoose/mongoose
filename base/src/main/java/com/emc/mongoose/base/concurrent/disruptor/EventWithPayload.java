package com.emc.mongoose.base.concurrent.disruptor;

public interface EventWithPayload<T> {

	void payload(final T payload);

	T payload();

	@Override
	String toString();
}
