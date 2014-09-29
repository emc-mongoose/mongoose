package com.emc.mongoose.object.load.driver.impl;
//
import com.emc.mongoose.base.data.persist.FrameBuffConsumer;
import com.emc.mongoose.object.api.WSObjectRequestConfig;
import com.emc.mongoose.object.data.impl.WSDataObjectBase;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
import com.emc.mongoose.object.load.driver.ObjectLoadService;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.load.impl.type.WSCreate;
import com.emc.mongoose.base.load.driver.ConsumerService;
import com.emc.mongoose.util.remote.RecordFrameBuffer;
import com.emc.mongoose.util.remote.Service;
import com.emc.mongoose.util.remote.ServiceUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
/**
 Created by kurila on 30.05.14.
 */
public final class WSCreateService<T extends WSDataObjectBase>
extends WSCreate<T>
implements ObjectLoadService<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public WSCreateService(
		final String[] addrs, final WSObjectRequestConfig<T> reqConf, final long maxCount,
		final int threadsPerNode, final long minObjSize, final long maxObjSize
	)
	throws IOException, CloneNotSupportedException {
		super(addrs, reqConf, maxCount, threadsPerNode, null, minObjSize, maxObjSize);
		// by default, may be overriden later externally:
		super.setConsumer(new FrameBuffConsumer<T>());
	}
	//
	@Override
	public final synchronized void close()
	throws IOException {
		super.close();
		// close the exposed network service, if any
		final Service svc = ServiceUtils.getLocalSvc(getName());
		if(svc==null) {
			LOG.debug(Markers.MSG, "The load was not exposed remotely");
		} else {
			LOG.debug(Markers.MSG, "The load was exposed remotely, removing the service");
			ServiceUtils.close(svc);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void setConsumer(final ConsumerService<T> consumer) {
		this.consumer = consumer;
		LOG.debug(Markers.MSG, "Trying to resolve local service from the name");
		try {
			final ConsumerService remoteSvc = ConsumerService.class.cast(consumer);
			final String remoteSvcName = remoteSvc.getName();
			LOG.debug(Markers.MSG, "Name is {}", remoteSvcName);
			final Service localSvc = ServiceUtils.getLocalSvc(remoteSvcName);
			if(localSvc==null) {
				LOG.error(Markers.ERR, "Failed to get local service for name {}", remoteSvcName);
			} else {
				super.setConsumer((ObjectLoadExecutor<T>) localSvc);
			}
			LOG.debug(Markers.MSG, "Successfully resolved local service and appended it as consumer");
		} catch(final IOException ee) {
			LOG.error(Markers.ERR, "Looks like network failure", ee);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final List<T> takeFrame()
	throws RemoteException {
		List<T> recFrame = Collections.emptyList();
		if(RecordFrameBuffer.class.isInstance(consumer)) {
			recFrame = ((RecordFrameBuffer<T>) consumer).takeFrame();
		}
		return recFrame;
	}
	//
}
