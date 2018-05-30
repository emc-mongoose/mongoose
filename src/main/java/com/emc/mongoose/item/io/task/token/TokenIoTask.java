package com.emc.mongoose.item.io.task.token;

import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.TokenItem;

/**
 Created by kurila on 11.07.16.
 */
public interface TokenIoTask<I extends TokenItem>
extends IoTask<I> {

	@Override
	I item();

	long getCountBytesDone();
	
	void setCountBytesDone(long n);
	
	long getRespDataTimeStart();
	
	void startDataResponse();
}
