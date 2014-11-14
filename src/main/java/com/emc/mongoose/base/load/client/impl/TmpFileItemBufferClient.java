package com.emc.mongoose.base.load.client.impl;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.persist.DataItemBuffer;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.client.DataItemBufferClient;
import com.emc.mongoose.base.load.client.LoadClient;
import com.emc.mongoose.base.load.server.LoadBuilderSvc;
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.util.logging.ExceptionHandler;
//
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
//
/**
 Created by kurila on 14.11.14.
 */
public class TmpFileItemBufferClient<T extends DataItem, U extends LoadExecutor<T>>
extends HashMap<String, DataItemBuffer<T>>
implements DataItemBufferClient<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Map<String, LoadBuilderSvc<T, U>> loadBuilderSvcMap;
	//
	public TmpFileItemBufferClient(final Map<String, LoadBuilderSvc<T, U>> loadBuilderSvcMap)
	throws RemoteException {
		//
		this.loadBuilderSvcMap = loadBuilderSvcMap;
		//
		LoadBuilderSvc<T, U> nextLoadBuilderSvc;
		DataItemBuffer<T> nextDataItemBuffer;
		for(final String addr: loadBuilderSvcMap.keySet()) {
			nextLoadBuilderSvc = loadBuilderSvcMap.get(addr);
			nextDataItemBuffer = null;
			try {
				nextDataItemBuffer = nextLoadBuilderSvc.newDataItemBuffer();
			} catch(final IOException e) {
				ExceptionHandler.trace(
					LOG, Level.ERROR, e,
					String.format("Failed to create remote data item buffer @%s", addr)
				);
			}
			put(addr, nextDataItemBuffer);
		}
	}
	//
	@Override
	public void submit(final T data)
	throws RemoteException {
		throw new RemoteException("Not supported in distributed mode currently");
	}
	//
	@Override
	public final long getMaxCount()
	throws RemoteException {
		return loadBuilderSvcMap.get(
			loadBuilderSvcMap.keySet().iterator().next()
		).getMaxCount();
	}
	//
	@Override
	public final void setMaxCount(final long maxCount)
	throws RemoteException {
		DataItemBuffer<T> nextDataItemBuffer;
		for(final String addr: keySet()) {
			nextDataItemBuffer = get(addr);
			if(nextDataItemBuffer != null) {
				nextDataItemBuffer.setMaxCount(maxCount);
			}
		}
	}
	//
	@Override
	public final void close()
	throws IOException {
		DataItemBuffer<T> nextDataItemBuffer;
		for(final String addr: keySet()) {
			nextDataItemBuffer = get(addr);
			if(nextDataItemBuffer != null) {
				nextDataItemBuffer.close();
			}
		}
	}
	//
	@Override
	public void writeExternal(final ObjectOutput objectOutput)
	throws IOException {
		throw new RemoteException("Not supported in distributed mode currently");
	}
	//
	@Override
	public void readExternal(final ObjectInput objectInput)
	throws IOException, ClassNotFoundException {
		throw new RemoteException("Not supported in distributed mode currently");
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer)
	throws RemoteException {
		if(LoadClient.class.isInstance(consumer)) {
			final Map<String, LoadSvc<T>> loadSvcMap = ((LoadClient<T>) consumer).getRemoteLoadMap();
			DataItemBuffer<T> nextDataItemBuffer;
			LoadSvc<T> nextLoadSvc;
			for(final String addr: keySet()) {
				nextDataItemBuffer = get(addr);
				nextLoadSvc = loadSvcMap.get(addr);
				nextDataItemBuffer.setConsumer(nextLoadSvc);
			}
		} else {
			LOG.warn(
				Markers.ERR, "Attempted to set the invalid-type consumer: {}",
				consumer == null ? null : consumer.getClass().getCanonicalName()
			);
		}
	}
	//
	@Override
	public Consumer<T> getConsumer()
	throws RemoteException {
		throw new RemoteException("Not supported in distributed mode currently");
	}
	//
	@Override
	public final void start()
	throws RemoteException {
		DataItemBuffer<T> nextDataItemBuffer;
		for(final String addr: keySet()) {
			nextDataItemBuffer = get(addr);
			if(nextDataItemBuffer != null) {
				nextDataItemBuffer.start();
			}
		}
	}
	//
	@Override
	public final void interrupt()
	throws RemoteException {
		DataItemBuffer<T> nextDataItemBuffer;
		for(final String addr: keySet()) {
			nextDataItemBuffer = get(addr);
			if(nextDataItemBuffer != null) {
				nextDataItemBuffer.interrupt();
			}
		}
	}
}
