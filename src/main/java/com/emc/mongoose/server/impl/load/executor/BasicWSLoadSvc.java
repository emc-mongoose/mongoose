package com.emc.mongoose.server.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.executor.BasicWSLoadExecutor;
// mongoose-server-impl.jar
import com.emc.mongoose.server.impl.load.model.FrameBuffConsumer;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.model.ConsumerSvc;
import com.emc.mongoose.server.api.load.model.RecordFrameBuffer;
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
//
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
		final int threadsPerNode, final String listFile, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias, final float rateLimit,
		final int countUpdPerReq
	) {
		super(
			runTimeConfig, reqConfig, addrs, threadsPerNode, listFile, maxCount,
			sizeMin, sizeMax, sizeBias, rateLimit, countUpdPerReq
		);
		// by default, may be overriden later externally:
		super.setConsumer(new FrameBuffConsumer<>(dataCls, runTimeConfig, maxCount));
	}
	//
	@Override
	public final void close()
	throws IOException {
		super.close();
		// close the exposed network service, if any
		final Service svc = ServiceUtils.getLocalSvc(getName());
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
				final Service localSvc = ServiceUtils.getLocalSvc(remoteSvcName);
				if(localSvc == null) {
					LOG.error(Markers.ERR, "Failed to get local service for name {}", remoteSvcName);
				} else {
					super.setConsumer((WSLoadExecutor<T>) localSvc);
				}
			}
			LOG.debug(Markers.MSG, "Successfully resolved local service and appended it as consumer");
		} catch(final IOException ee) {
			LOG.error(Markers.ERR, "Looks like network failure", ee);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final Collection<T> takeFrame()
	throws RemoteException, InterruptedException {
		Collection<T> recFrame = null;
		if(consumer != null && RecordFrameBuffer.class.isInstance(consumer)) {
			recFrame = ((RecordFrameBuffer<T>) consumer).takeFrame();
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "Returning {} data items records",
				recFrame == null ? 0 : recFrame.size()
			);
		}
		return recFrame;
	}
	//
	@Override
	public final int getInstanceNum() {
		return instanceNum;
	}
	//
    @Override
    public final long[] getLatencyValues() {
        return super.getLatencyValues();
    }
}
