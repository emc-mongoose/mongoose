package com.emc.mongoose.storage.mock.impl.web;
//
import com.emc.mongoose.common.collections.InstancePool;
//
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.WSMockTask;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.base.BasicObjectMockTask;
//
import java.util.Map;
/**
 Created by kurila on 27.07.15.
 */
public final class BasicWSMockTask<T extends WSObjectMock>
extends BasicObjectMockTask<T>
implements WSMockTask<T> {
	//
	private final static InstancePool<BasicWSMockTask> TASK_POOL = new InstancePool<>(
		BasicWSMockTask.class
	);
	//
	public static <T extends DataObjectMock> BasicWSMockTask getInstance(
		final Map<String, T> index, final String id, final IOTask.Type type,
		final long offset, final long size
	) {
		return TASK_POOL.take(index, id, type, offset, size);
	}
	//
	@Override
	public final void release() {
		TASK_POOL.release(this);
	}
	//
	@Override
	protected final T createNew(final String id, final long offset, final long size) {
		return (T) new BasicWSObjectMock(id, offset, size);
	}
}
