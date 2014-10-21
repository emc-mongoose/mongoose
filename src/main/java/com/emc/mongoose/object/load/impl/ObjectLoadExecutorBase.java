package com.emc.mongoose.object.load.impl;
//
import com.emc.mongoose.base.load.impl.LoadExecutorBase;
import com.emc.mongoose.object.api.ObjectRequestConfig;
import com.emc.mongoose.object.data.DataObject;
//
import com.emc.mongoose.object.load.ObjectLoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 09.10.14.
 */
public abstract class ObjectLoadExecutorBase<T extends DataObject>
extends LoadExecutorBase<T>
implements ObjectLoadExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@SuppressWarnings("unchecked")
	protected ObjectLoadExecutorBase(
		final RunTimeConfig runTimeConfig,
		final String[] addrs, final ObjectRequestConfig<T> reqConf, final long maxCount,
		final int threadsPerNode, final String listFile
	) throws ClassCastException {
		super(runTimeConfig, addrs, reqConf, maxCount, threadsPerNode, listFile);
	}
	//
}
