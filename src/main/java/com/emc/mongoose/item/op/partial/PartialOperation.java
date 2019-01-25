package com.emc.mongoose.item.op.partial;

import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.op.composite.CompositeOperation;

/** Created by andrey on 23.11.16. */
public interface PartialOperation<I extends Item> extends Operation<I> {

  int partNumber();

  CompositeOperation<I> parent();
}
