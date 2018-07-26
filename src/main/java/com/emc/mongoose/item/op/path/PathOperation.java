package com.emc.mongoose.item.op.path;

import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.PathItem;

/**
 Created by kurila on 30.01.17.
 */
public interface PathOperation<I extends PathItem>
extends Operation<I> {

	@Override
	I item();

	long countBytesDone();

	void countBytesDone(long n);

	long respDataTimeStart();

	void startDataResponse();
}
