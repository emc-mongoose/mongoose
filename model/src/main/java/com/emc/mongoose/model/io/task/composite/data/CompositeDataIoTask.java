package com.emc.mongoose.model.io.task.composite.data;

import com.emc.mongoose.model.io.task.composite.CompositeIoTask;
import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import static com.emc.mongoose.model.io.task.composite.CompositeIoTask.CompositeIoResult;
import com.emc.mongoose.model.io.task.partial.data.PartialDataIoTask;
import com.emc.mongoose.model.item.DataItem;

import java.util.List;
/**
 Created by andrey on 25.11.16.
 */
public interface CompositeDataIoTask<
	I extends DataItem, R extends CompositeDataIoTask.CompositeDataIoResult<I>
>
extends CompositeIoTask<I, R> {
	
	interface CompositeDataIoResult<I extends DataItem>
	extends DataIoResult<I>, CompositeIoResult<I> {
	}
	
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

	@Override
	List<? extends PartialDataIoTask> getSubTasks();
}
