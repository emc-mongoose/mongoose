package com.emc.mongoose.server.impl.load.executor;
//
import com.emc.mongoose.core.impl.load.executor.BasicWSLoadExecutor;
//
import com.emc.mongoose.server.impl.load.model.FrameBuffConsumer;
//
import com.emc.mongoose.server.api.load.model.ConsumerSvc;
import com.emc.mongoose.server.api.load.model.RecordFrameBuffer;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
//
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
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
		final int threadsPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final int countUpdPerReq
	) {
		super(
			runTimeConfig, reqConfig, addrs, threadsPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias, countUpdPerReq
		);
		// by default, may be overriden later externally:
		super.setConsumer(new FrameBuffConsumer<T>());
	}
	//
	@Override
	public final void close()
	throws IOException {
		super.close();
		// close the exposed network service, if any
		final Service svc = ServiceUtils.getLocalSvc(getName());
		if(svc == null) {
			LOG.debug(LogUtil.MSG, "The load was not exposed remotely");
		} else {
			LOG.debug(LogUtil.MSG, "The load was exposed remotely, removing the service");
			ServiceUtils.close(svc);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void setConsumer(final ConsumerSvc<T> consumer) {
		LOG.debug(
			LogUtil.MSG, "Set consumer {} for {}, trying to resolve local service from the name",
			consumer, getName()
		);
		this.consumer = consumer;
		try {
			final ConsumerSvc remoteSvc = ConsumerSvc.class.cast(consumer);
			final String remoteSvcName = remoteSvc.getName();
			LOG.debug(LogUtil.MSG, "Name is {}", remoteSvcName);
			final Service localSvc = ServiceUtils.getLocalSvc(remoteSvcName);
			if(localSvc == null) {
				LOG.error(LogUtil.ERR, "Failed to get local service for name {}", remoteSvcName);
			} else {
				super.setConsumer((ObjectLoadExecutor<T>) localSvc);
			}
			LOG.debug(LogUtil.MSG, "Successfully resolved local service and appended it as consumer");
		} catch(final IOException ee) {
			LOG.error(LogUtil.ERR, "Looks like network failure", ee);
		}
	}
	//
	private final static Object[] EMPTY_FRAME = new Object[0];
	//
	@Override @SuppressWarnings("unchecked")
	public final T[] takeFrame()
	throws RemoteException {
		final T[] recFrame;
		if(RecordFrameBuffer.class.isInstance(consumer)) {
			recFrame = ((RecordFrameBuffer<T>) consumer).takeFrame();
		} else {
			recFrame = (T[]) EMPTY_FRAME;
		}
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, "Returning {} data items records", recFrame.length);
		}
		return recFrame;
	}
	//
	@Override
	public final int getTotalConnCount()
	throws RemoteException {
		return connCountPerNode * storageNodeAddrs.length;
	}
	//

}
