package com.emc.mongoose.core.api.load.model;
/**
 Created by kurila on 16.06.15.
 */
public interface AccumulatorProducer<T>
extends AsyncConsumer<T>, Producer<T> {
	/** @return The count of the items accumulated successfully. */
	long getCount();
}
