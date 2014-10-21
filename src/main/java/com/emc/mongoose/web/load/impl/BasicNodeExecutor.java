package com.emc.mongoose.web.load.impl;
//
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.object.load.impl.ObjectNodeExecutorBase;
import com.emc.mongoose.web.api.impl.BasicWSRequest;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 06.06.14.
 */
public final class BasicNodeExecutor<T extends WSObject>
extends ObjectNodeExecutorBase<T>
implements com.emc.mongoose.web.load.WSNodeExecutor<T> {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	public BasicNodeExecutor(
		final RunTimeConfig runTimeConfig,
		final String addr, final int threadsPerNode, final WSRequestConfig<T> sharedReqConf,
		final MetricRegistry parentMetrics, final String parentName
	) throws CloneNotSupportedException {
		super(runTimeConfig, addr, threadsPerNode, sharedReqConf, parentMetrics, parentName);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final com.emc.mongoose.web.api.WSRequest getRequestFor(final T dataItem) {
		return (com.emc.mongoose.web.api.WSRequest) BasicWSRequest.getInstanceFor(localReqConf, dataItem);
	}
	//
}
