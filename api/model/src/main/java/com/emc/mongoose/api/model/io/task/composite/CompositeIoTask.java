package com.emc.mongoose.api.model.io.task.composite;

import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.api.model.item.Item;

import java.util.List;

/**
 Created by andrey on 25.11.16.
 Marker interface
 */
public interface CompositeIoTask<I extends Item>
extends IoTask<I> {
	
	@Override
	I item();
	
	String get(final String key);

	void put(final String key, final String value);

	List<? extends PartialIoTask> subTasks();

	/** Should be invoked only after subTasks() **/
	void markSubTaskCompleted();

	boolean allSubTasksDone();
}
