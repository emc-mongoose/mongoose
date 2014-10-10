package com.emc.mongoose.object.load;
//
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.object.api.DataObjectRequest;
import com.emc.mongoose.object.api.WSRequest;
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.api.WSRequestImpl;
import com.emc.mongoose.object.data.WSObject;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
/**
 Created by kurila on 06.06.14.
 */
public final class WSNodeExecutorImpl<T extends WSObject>
extends ObjectNodeExecutorBase<T>
implements WSNodeExecutor<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public WSNodeExecutorImpl(
		final String addr, final int threadsPerNode, final WSRequestConfig<T> sharedReqConf,
		final MetricRegistry parentMetrics, final String parentName
	) throws CloneNotSupportedException {
		super(addr, threadsPerNode, sharedReqConf, parentMetrics, parentName);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final WSRequest<T> getRequestFor(final T dataItem) {
		return (WSRequest<T>) WSRequestImpl.getInstanceFor(
			(WSRequestConfig<T>) localReqConf, dataItem
		);
	}
	//
	@Override
	protected final boolean isResponseValid(final DataObjectRequest<T> request) {
		return request.getStatusCode() < 300;
	}
	//
}
