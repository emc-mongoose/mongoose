package com.emc.mongoose.item.op.path;

import com.emc.mongoose.item.op.OperationsBuilder;
import com.emc.mongoose.item.PathItem;

/**
 Created by andrey on 31.01.17.
 */
public interface PathOperationsBuilder<I extends PathItem, O extends PathOperation<I>>
extends OperationsBuilder<I, O> {
}
