package com.emc.mongoose.item.op.token;

import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.TokenItem;

/**
 Created by kurila on 11.07.16.
 */
public interface TokenOperation<I extends TokenItem>
extends Operation<I> {

	@Override
	I item();

	long countBytesDone();
	
	void countBytesDone(long n);
	
	long respDataTimeStart();
	
	void startDataResponse();
}
