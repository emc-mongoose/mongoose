package com.emc.mongoose.core.api.load.model;
//
import com.emc.mongoose.core.api.data.DataItem;
/**
 Created by kurila on 16.06.15.
 */
public interface AccumulatorProducer<T extends DataItem>
extends AsyncConsumer<T>, Producer<T> {
	/** @return The count of the items accumulated successfully. */
	long getCount();
}
