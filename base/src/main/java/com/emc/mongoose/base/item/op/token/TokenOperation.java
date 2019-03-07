package com.emc.mongoose.base.item.op.token;

import com.emc.mongoose.base.item.TokenItem;
import com.emc.mongoose.base.item.op.Operation;

/** Created by kurila on 11.07.16. */
public interface TokenOperation<I extends TokenItem> extends Operation<I> {

	@Override
	I item();

	long countBytesDone();

	void countBytesDone(long n);

	long respDataTimeStart();

	void startDataResponse();
}
