package com.emc.mongoose.object.http.driver.impl;
//
import com.emc.mongoose.data.persist.FrameBuffConsumer;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.WSLoadExecutor;
import com.emc.mongoose.object.http.data.WSObject;
import com.emc.mongoose.object.http.api.WSRequestConfig;
import com.emc.mongoose.object.http.impl.Update;
import com.emc.mongoose.remote.ConsumerService;
import com.emc.mongoose.remote.LoadService;
import com.emc.mongoose.remote.RecordFrameBuffer;
import com.emc.mongoose.remote.Service;
import com.emc.mongoose.remote.ServiceUtils;
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
public final class UpdateService
extends Update
implements LoadService<WSObject> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public UpdateService(
		final String[] addrs, final WSRequestConfig reqConf, final long maxCount,
		final int threadsPerNode, final int updatesPerObject
	) {
		super(addrs, reqConf, maxCount, threadsPerNode, null, updatesPerObject);
		// by default, may be overriden later externally:
		super.setConsumer(new FrameBuffConsumer<WSObject>(reqConf.getDataSource()));
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
	@Override
	public final void setConsumer(final ConsumerService<WSObject> consumer) {
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
				super.setConsumer(WSLoadExecutor.class.cast(localSvc));
			}
			LOG.debug(Markers.MSG, "Successfully resolved local service and appended it as consumer");
		} catch(final IOException ee) {
			LOG.error(Markers.ERR, "Looks like network failure", ee);
		}
	}
	//
	@Override
	@SuppressWarnings("unchecked")
	public final List<WSObject> takeFrame()
		throws RemoteException {
		List<WSObject> recFrame = Collections.emptyList();
		if(RecordFrameBuffer.class.isInstance(consumer)) {
			recFrame = ((RecordFrameBuffer<WSObject>) consumer).takeFrame();
		}
		return recFrame;
	}
	//
}
