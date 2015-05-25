package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.core.api.io.task.DataObjectIOTask;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.common.collections.InstancePool;
/**
 Created by kurila on 23.12.14.
 */
public class BasicObjectIOTask<T extends DataObject>
extends BasicIOTask<T>
implements DataObjectIOTask<T> {
	// BEGIN pool related things
	private final static InstancePool<BasicObjectIOTask>
		POOL_OBJ_TASKS = new InstancePool<>(BasicObjectIOTask.class);
	//
	@SuppressWarnings("unchecked")
	public static <T extends DataObject> BasicObjectIOTask<T> getInstanceFor(
		final LoadExecutor<T> loadExecutor, final T dataItem, final String nodeAddr
	) {
		return (BasicObjectIOTask<T>) POOL_OBJ_TASKS.take(loadExecutor, dataItem, nodeAddr);
	}
	//
	@Override
	public void release() {
		POOL_OBJ_TASKS.release(this);
	}
	// END pool related things
}
