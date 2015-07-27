package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
//
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.storage.mock.api.DataObjectMock;
import com.emc.mongoose.storage.mock.api.ObjectStorageMockTask;
import com.emc.mongoose.storage.mock.api.StorageMockTask;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
/**
 Created by andrey on 27.07.15.
 */
public class BasicObjectMockTask<T extends DataObjectMock>
implements ObjectStorageMockTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static InstancePool<BasicObjectMockTask> TASK_POOL = new InstancePool<>(
		BasicObjectMockTask.class
	);
	//
	public static <T extends DataObjectMock> BasicObjectMockTask getInstance(
		final Map<String, T> index, final String id, final IOTask.Type type,
		final long offset, final long size
	) {
		return TASK_POOL.take(index, id, type, offset, size);
	}
	//
	protected Map<String, T> index;
	protected String id;
	protected IOTask.Type type;
	protected long offset, size;
	//
	@Override
	public final Reusable<StorageMockTask<T>> reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				index = (Map<String, T>) args[0];
			}
			if(args.length > 1) {
				id = String.class.cast(args[1]);
			}
			if(args.length > 2) {
				type = IOTask.Type.class.cast(args[2]);
			}
			if(args.length > 3) {
				offset = (long) args[3];
			}
			if(args.length > 4) {
				size = (long) args[4];
			}
		}
		return this;
	}
	//
	@Override
	public void release() {
		TASK_POOL.release(this);
	}
	//
	@Override
	public final T call() {
		T dataObject = null;
		switch(type) {
			case CREATE:
				dataObject = createNew(id, offset, size);
				index.put(id, dataObject);
				break;
			case READ:
				dataObject = index.get(id);
				break;
			case DELETE:
				dataObject = index.remove(id);
				break;
			case UPDATE:
				dataObject = index.get(id);
				if(dataObject != null) {
					dataObject.update(offset, size);
				}
				break;
			case APPEND:
				dataObject = index.get(id);
				if(dataObject != null) {
					dataObject.append(offset, size);
				}
				break;
		}
		return dataObject;
	}
	//
	protected T createNew(final String id, final long offset, final long size) {
		return (T) new BasicObjectMock(id, offset, size);
	}
}
