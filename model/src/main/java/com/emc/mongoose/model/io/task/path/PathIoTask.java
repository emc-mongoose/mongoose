package com.emc.mongoose.model.io.task.path;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.PathItem;

/**
 Created by kurila on 30.01.17.
 */
public interface PathIoTask<I extends PathItem>
extends IoTask<I> {

	@Override
	I getItem();
	
	long getCountBytesDone();
	
	void setCountBytesDone(long n);
	
	long getRespDataTimeStart();
	
	void startDataResponse();
}
