package com.emc.mongoose.item.op.partial;

import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.op.composite.CompositeOperation;
import com.emc.mongoose.item.Item;

/**
 Created by andrey on 23.11.16.
 */
public interface PartialOperation<I extends Item>
extends Operation<I> {
	
	int partNumber();

	CompositeOperation<I> parent();
}
