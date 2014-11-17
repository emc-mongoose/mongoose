package com.emc.mongoose.base.load.client.impl;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.DataItemBuffer;
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.client.DataItemBufferClient;
import com.emc.mongoose.base.load.client.LoadClient;
import com.emc.mongoose.base.load.server.DataItemBufferSvc;
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
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.11.14.
 */
public class TmpFileItemBufferClient<T extends DataItem, U extends LoadExecutor<T>>
extends HashMap<String, DataItemBufferSvc<T>>
implements DataItemBufferClient<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Map<String, LoadBuilderSvc<T, U>> loadBuilderSvcMap;
	//
	@SuppressWarnings("unchecked")
	public TmpFileItemBufferClient(final Map<String, LoadBuilderSvc<T, U>> loadBuilderSvcMap)
	throws RemoteException {
		//
		this.loadBuilderSvcMap = loadBuilderSvcMap;
		//
		LoadBuilderSvc<T, U> nextLoadBuilderSvc;
		DataItemBufferSvc<T> nextDataItemBuffer;
		//
		for(final String addr: loadBuilderSvcMap.keySet()) {
			nextLoadBuilderSvc = loadBuilderSvcMap.get(addr);
			nextDataItemBuffer = null;
			try {
				nextDataItemBuffer = (DataItemBufferSvc<T>) nextLoadBuilderSvc.newDataItemBuffer();
			} catch(final IOException e) {
				ExceptionHandler.trace(
					LOG, Level.ERROR, e,
					String.format("Failed to create remote data items buffer @%s", addr)
				);
			}
			put(addr, nextDataItemBuffer);
		}
	}
	//
	@Override
	public void submit(final T data)
	throws RemoteException {
		throw new RemoteException("The method is not supported in distributed mode currently");
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
				LOG.debug(
					Markers.MSG, "Closed remote date item buffer for output @{}: \"{}\"",
					addr, nextDataItemBuffer.toString()
				);
			}
		}
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
		throw new RemoteException("The method is not supported in distributed mode currently");
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
				LOG.debug(
					Markers.MSG, "Started producing from remote data items buffer @{}: \"{}\"",
					addr, nextDataItemBuffer.toString()
				);
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
				LOG.info(
					Markers.MSG, "Interrupted producing from remote data items buffer @{}: \"{}\"",
					addr, nextDataItemBuffer.toString()
				);
			}
		}
	}
	//
	@Override
	public final void join(final long milliSec)
	throws InterruptedException {
		//
		final ExecutorService remoteJoinExecutor = Executors.newFixedThreadPool(size());
		//
		for(final String addr: keySet()) {
			final DataItemBuffer<T> nextDataItemBuffer = get(addr);
			if(nextDataItemBuffer != null) {
				remoteJoinExecutor.submit(
					new Runnable() {
						@Override
						public void run() {
							try {
								nextDataItemBuffer.join(milliSec);
								LOG.debug(
									Markers.MSG,
									"Finished the remote data items buffer producing @{}: \"{}\"",
									addr, nextDataItemBuffer.toString()
								);
							} catch(final InterruptedException e) {
								LOG.debug(Markers.MSG, "Interrupted");
							} catch(final IOException e) {
								ExceptionHandler.trace(
									LOG, Level.WARN, e,
									String.format(
										"Failed to await the remote data items buffer @%s", addr
									)
								);
							}
						}
					}
				);
			} else {
				throw new IllegalStateException(
					String.format(
						"There is a null pointer in the remote buffers map for address \"%s\"", addr
					)
				);
			}
		}
		//
		remoteJoinExecutor.shutdown();
		remoteJoinExecutor.awaitTermination(milliSec, TimeUnit.MILLISECONDS);
	}
}
