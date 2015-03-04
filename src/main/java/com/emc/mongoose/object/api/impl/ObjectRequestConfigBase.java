package com.emc.mongoose.object.api.impl;
//
import com.emc.mongoose.base.api.impl.RequestConfigBase;
import com.emc.mongoose.object.api.ObjectRequestConfig;
import com.emc.mongoose.object.data.DataObject;
/**
 Created by kurila on 23.12.14.
 */
public abstract class ObjectRequestConfigBase<T extends DataObject>
extends RequestConfigBase<T>
implements ObjectRequestConfig<T> {
	//
	@Override
	public BasicObjectIOTask<T> getRequestFor(T dataItem, String nodeAddr)
	throws InterruptedException {
		return (BasicObjectIOTask<T>) BasicObjectIOTask.getInstanceFor(this, dataItem, nodeAddr);
	}
	//
	/*@Override
	public Producer<T> getAnyDataProducer(long maxCount, LoadExecutor<T> loadExecutor) {
		return null;
	}
	//
	@Override
	public void configureStorage(LoadExecutor<T> loadExecutor) throws IllegalStateException {
	}*/
}
