package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.core.api.io.req.conf.ObjectRequestConfig;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.impl.io.task.BasicObjectIOTask;
//
import java.io.IOException;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
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
		final int connCountPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final float rateLimit,
		final int countUpdPerReq
	) throws ClassCastException {
		super(
			dataCls,
			runTimeConfig, reqConfig, addrs, connCountPerNode, listFile, maxCount, sizeMin, sizeMax,
			sizeBias, rateLimit, countUpdPerReq
		);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected BasicObjectIOTask<T> getIOTask(final T dataItem, final String nextNodeAddr) {
		return BasicObjectIOTask.getInstance(this, dataItem, nextNodeAddr);
	}
	//
	@Override
	public void close()
	throws IOException {
		try {
			super.close();
		} finally {
			BasicObjectIOTask.INSTANCE_POOL_MAP.put(this, null); // dispose the I/O tasks pool
		}
	}
}
