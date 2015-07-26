package com.emc.mongoose.storage.mock.impl.base;
//
import com.emc.mongoose.common.collections.Reusable;
//
import com.emc.mongoose.core.api.data.DataObject;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.storage.mock.api.StorageMockTask;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
/**
 Created by andrey on 27.07.15.
 */
public abstract class ObjectStorageMockTaskBase<T extends DataObject>
implements StorageMockTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected Map<String, T> index;
	protected String id;
	protected IOTask.Type type;
	protected long offset, size;
	//
	@Override
	public Reusable<StorageMockTask<T>> reuse(final Object... args)
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
	public T call() {
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
				// TODO update
				break;
			case APPEND:
				dataObject = index.get(id);
				// TODO append
				break;
		}
		return dataObject;
	}
	//
	protected abstract T createNew(final String id, final long offset, final long size);
}
