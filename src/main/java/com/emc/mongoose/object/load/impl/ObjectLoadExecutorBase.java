package com.emc.mongoose.object.load.impl;
//
import com.emc.mongoose.base.load.impl.AdvancedLoadExecutorBase;
import com.emc.mongoose.object.api.ObjectRequestConfig;
import com.emc.mongoose.object.data.DataObject;
//
import com.emc.mongoose.object.load.ObjectLoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 09.10.14.
 */
public abstract class ObjectLoadExecutorBase<T extends DataObject>
extends AdvancedLoadExecutorBase<T>
implements ObjectLoadExecutor<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	protected ObjectLoadExecutorBase(
		final RunTimeConfig runTimeConfig, final ObjectRequestConfig<T> reqConfig, final String[] addrs,
		final int threadsPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final int countUpdPerReq
	) throws ClassCastException {
		super(
			runTimeConfig, reqConfig, addrs, threadsPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias, countUpdPerReq
		);
	}
	//
}
