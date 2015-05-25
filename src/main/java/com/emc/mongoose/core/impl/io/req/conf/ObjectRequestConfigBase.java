package com.emc.mongoose.core.impl.io.req.conf;
//
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.io.req.conf.ObjectRequestConfig;
//
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
import com.emc.mongoose.core.impl.io.task.BasicObjectIOTask;
/**
 Created by kurila on 23.12.14.
 */
public abstract class ObjectRequestConfigBase<T extends DataObject>
extends RequestConfigBase<T>
implements ObjectRequestConfig<T> {
	//
	protected ObjectRequestConfigBase(final ObjectRequestConfig<T> reqConf2Clone) {
		super(reqConf2Clone);
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
