package com.emc.mongoose.server.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.executor.BasicWSLoadExecutor;
// mongoose-server-impl.jar
import com.emc.mongoose.server.impl.load.model.BasicItemBuffDst;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.model.ConsumerSvc;
import com.emc.mongoose.server.api.load.model.RemoteItemBuffDst;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
/**
 Created by kurila on 16.12.14.
 */
public final class BasicWSLoadSvc<T extends WSObject>
extends BasicWSLoadExecutor<T>
implements WSLoadSvc<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSLoadSvc(
		final RunTimeConfig runTimeConfig, final WSRequestConfig<T> reqConfig, final String[] addrs,
		final int connPerNode, final int threadsPerNode,
		final DataItemSrc<T> itemSrc, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final float rateLimit,
		final int countUpdPerReq
	) {
		super(
			runTimeConfig, reqConfig, addrs, connPerNode, threadsPerNode, itemSrc, maxCount,
			sizeMin, sizeMax, sizeBias, rateLimit, countUpdPerReq
		);
		// by default, may be overriden later externally:
		super.setDataItemDst(
			new BasicItemBuffDst<T>(rtConfig.getTasksMaxQueueSize(), rtConfig.getBatchSize())
		);
	}
	//
	@Override
	public final void close()
	throws IOException {
		super.close();
		//
		if(consumer instanceof RemoteItemBuffDst) {
			consumer.close();
		}
		// close the exposed network service, if any
		final Service svc = ServiceUtils.getLocalSvc(
			ServiceUtils.getLocalSvcName(getName())
		);
		if(svc == null) {
			LOG.debug(Markers.MSG, "The load was not exposed remotely");
		} else {
			LOG.debug(Markers.MSG, "The load was exposed remotely, removing the service");
			ServiceUtils.close(svc);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void setConsumer(final ConsumerSvc<T> consumer) {
		LOG.debug(
			Markers.MSG, "Set consumer {} for {}, trying to resolve local service from the name",
			consumer, getName()
		);
		this.consumer = consumer;
		try {
			if(consumer != null) {
				final String remoteSvcName = consumer.getName();
				LOG.debug(Markers.MSG, "Name is {}", remoteSvcName);
				final Service localSvc = ServiceUtils.getLocalSvc(
					ServiceUtils.getLocalSvcName(remoteSvcName)
				);
				if(localSvc == null) {
					LOG.error(Markers.ERR, "Failed to get local service for name {}", remoteSvcName);
				} else {
					super.setDataItemDst(localSvc);
					super.setConsumer((WSLoadExecutor<T>) localSvc);
				}
			}
			LOG.debug(Markers.MSG, "Successfully resolved local service and appended it as consumer");
		} catch(final IOException ee) {
			LOG.error(Markers.ERR, "Looks like network failure", ee);
		}
	}
	//
	@Override
	public final Collection<T> fetchItems()
	throws RemoteException {
		Collection<T> itemsBuff = null;
		if(consumer instanceof RemoteItemBuffDst) {
			try {
				itemsBuff = ((RemoteItemBuffDst<T>) consumer).fetchItems();
			} catch (final InterruptedException e) {
				if(!isShutdown.get()) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to fetch the frame");
				}
			}

		}
		return itemsBuff;
	}
	//
	@Override
	public final int getInstanceNum() {
		return instanceNum;
	}
	//
}
