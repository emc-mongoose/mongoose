package com.emc.mongoose.base.item.op.token;

import com.emc.mongoose.base.item.TokenItem;
import com.emc.mongoose.base.item.op.OperationsBuilder;

/** Created by kurila on 14.07.16. */
public interface TokenOperationsBuilder<I extends TokenItem, O extends TokenOperation<I>>
				extends OperationsBuilder<I, O> {}
