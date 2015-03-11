package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.io.task.DataObjectIOTask;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.impl.util.InstancePool;
/**
 Created by kurila on 23.12.14.
 */
public class BasicObjectIOTask<T extends DataObject>
extends BasicIOTask<T>
implements DataObjectIOTask<T> {
	//
	public final static BasicObjectIOTask POISON = new BasicObjectIOTask() {
		@Override
		public final String toString() {
			return "<POISON>";
		}
	};
	// BEGIN pool related things
	private final static InstancePool<BasicIOTask>
		POOL_OBJ_TASKS = new InstancePool<>(BasicIOTask.class);
	//
	@SuppressWarnings("unchecked")
	public static <T extends DataObject> BasicIOTask<T> getInstanceFor(
		final RequestConfig<T> reqConf, final T dataItem, final String nodeAddr
	) throws InterruptedException {
		return (BasicIOTask<T>) POOL_OBJ_TASKS.take(reqConf, dataItem, nodeAddr);
	}
	//
	@Override
	public void release() {
		if(isAvailable.compareAndSet(false, true)) {
			POOL_OBJ_TASKS.release(this);
		}
	}
	// END pool related things
}
