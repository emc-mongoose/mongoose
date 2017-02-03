package com.emc.mongoose.model.io.task.partial;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import com.emc.mongoose.model.item.Item;

/**
 Created by andrey on 23.11.16.
 Attention: not more serializable/externalizable!
 The partial I/O tasks should be constructed locally and should NOT be transferred via I/O channels.
 */
public interface PartialIoTask<I extends Item, R extends PartialIoTask.PartialIoResult>
extends IoTask<I, R> {
	
	interface PartialIoResult<I extends Item>
	extends IoResult<I> {
	}
	
	int getPartNumber();

	CompositeIoTask<I, ? extends IoResult> getParent();
}
