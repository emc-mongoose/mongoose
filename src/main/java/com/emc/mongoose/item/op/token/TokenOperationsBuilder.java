package com.emc.mongoose.item.op.token;

import com.emc.mongoose.item.op.OperationsBuilder;
import com.emc.mongoose.item.TokenItem;

/**
 Created by kurila on 14.07.16.
 */
public interface TokenOperationsBuilder<I extends TokenItem, O extends TokenOperation<I>>
extends OperationsBuilder<I, O> {
}
