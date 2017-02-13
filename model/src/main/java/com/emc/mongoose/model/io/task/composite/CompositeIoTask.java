package com.emc.mongoose.model.io.task.composite;

import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.io.task.partial.PartialIoTask;
import com.emc.mongoose.model.item.Item;

import java.util.List;

/**
 Created by andrey on 25.11.16.
 Marker interface
 */
public interface CompositeIoTask<I extends Item, R extends CompositeIoTask.CompositeIoResult<I>>
extends IoTask<I, R> {
	
	interface CompositeIoResult<I extends Item>
	extends IoResult<I> {
		boolean getCompleteFlag();
	}
	
	@Override
	I getItem();
	
	@Override
	R getResult(
		final String hostAddr,
		final boolean useStorageDriverResult,
		final boolean useStorageNodeResult,
		final boolean useItemInfoResult,
		final boolean useIoTypeCodeResult,
		final boolean useStatusCodeResult,
		final boolean useReqTimeStartResult,
		final boolean useDurationResult,
		final boolean useRespLatencyResult,
		final boolean useDataLatencyResult,
		final boolean useTransferSizeResult
	);

	String get(final String key);

	void put(final String key, final String value);

	List<? extends PartialIoTask> getSubTasks();

	/** Should be invoked only after getSubTasks() **/
	void subTaskCompleted();

	boolean allSubTasksDone();
}
