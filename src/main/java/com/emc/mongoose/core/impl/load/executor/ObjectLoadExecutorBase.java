package com.emc.mongoose.core.impl.load.executor;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.io.req.ObjectRequestConfig;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.io.task.DataObjectIOTask;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.io.task.BasicObjectIOTask;
//
//import org.apache.log.log4j.LogManager;
//import org.apache.log.log4j.Logger;
/**
 Created by kurila on 09.10.14.
 */
public abstract class ObjectLoadExecutorBase<T extends DataObject>
extends TypeSpecificLoadExecutorBase<T>
implements ObjectLoadExecutor<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	protected ObjectLoadExecutorBase(
		final Class<T> dataCls,
		final RunTimeConfig runTimeConfig, final ObjectRequestConfig<T> reqConfig, final String[] addrs,
		final int connCountPerNode, final DataItemInput<T> itemSrc, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final float rateLimit,
		final int countUpdPerReq
	) {
		super(
			dataCls,
			runTimeConfig, reqConfig, addrs, connCountPerNode, itemSrc, maxCount, sizeMin, sizeMax,
			sizeBias, rateLimit, countUpdPerReq
		);
	}
	//
	@Override
	protected DataObjectIOTask<T> getIOTask(final T dataItem, final String nextNodeAddr) {
		return new BasicObjectIOTask<>(this, dataItem, nextNodeAddr);
	}
}
