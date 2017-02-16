package com.emc.mongoose.model.io.task.token;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.TokenItem;

/**
 Created by kurila on 11.07.16.
 */
public interface TokenIoTask<I extends TokenItem>
extends IoTask<I> {

	@Override
	I getItem();

	long getCountBytesDone();
	
	void setCountBytesDone(long n);
	
	long getRespDataTimeStart();
	
	void startDataResponse();
}
