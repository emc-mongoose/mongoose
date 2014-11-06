package com.emc.mongoose.object.load.impl;
//
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.base.load.impl.StorageNodeExecutorBase;
import com.emc.mongoose.object.api.ObjectRequestConfig;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.object.load.ObjectNodeExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import java.util.Map;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 10.10.14.
 */
public abstract class ObjectNodeExecutorBase<T extends DataObject>
extends StorageNodeExecutorBase<T>
implements ObjectNodeExecutor<T> {
	//
	//private final static Logger log = LogManager.getLogger();
	//
	protected ObjectNodeExecutorBase(
		final RunTimeConfig runTimeConfig,
		final String addr, final int threadsPerNode, final ObjectRequestConfig<T> sharedReqConf,
		final MetricRegistry parentMetrics, final String parentName, final Map<String,String> context
	) {
		super(runTimeConfig, addr, threadsPerNode, sharedReqConf, parentMetrics, parentName, context);
	}
	//
}
