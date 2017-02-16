package com.emc.mongoose.model.io.task.partial;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.item.Item;

/**
 Created by andrey on 23.11.16.
 */
public interface PartialIoTask<I extends Item>
extends IoTask<I> {
	
	int getPartNumber();

	CompositeIoTask<I> getParent();
}
