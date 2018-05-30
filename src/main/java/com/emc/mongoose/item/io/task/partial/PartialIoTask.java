package com.emc.mongoose.item.io.task.partial;

import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.io.task.composite.CompositeIoTask;
import com.emc.mongoose.item.Item;

/**
 Created by andrey on 23.11.16.
 */
public interface PartialIoTask<I extends Item>
extends IoTask<I> {
	
	int partNumber();

	CompositeIoTask<I> parent();
}
