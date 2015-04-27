package com.emc.mongoose.client.impl.persist;
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.persist.DataItemBuffer;
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.persist.DataItemBufferSvc;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-client.jar
import com.emc.mongoose.client.api.persist.DataItemBufferClient;
import com.emc.mongoose.client.api.load.executor.LoadClient;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
				LogUtil.failure(
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
	public final void shutdown()
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
	public final void close() {
		DataItemBuffer<T> nextDataItemBuffer;
		for(final String addr: keySet()) {
			try {
				nextDataItemBuffer = get(addr);
				nextDataItemBuffer.close();
				LOG.debug(
					LogUtil.MSG, "Closed remote date item buffer for output @{}: \"{}\"",
					addr, nextDataItemBuffer.toString()
				);
			} catch(final Exception e) {
				LogUtil.failure(
					LOG, Level.WARN, e,
					String.format("Failed to close remote data items buffer @ %s", addr)
				);
			}
		}
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		if(LoadClient.class.isInstance(consumer)) {
			final Map<String, LoadSvc<T>> loadSvcMap = ((LoadClient<T>) consumer).getRemoteLoadMap();
			DataItemBuffer<T> nextDataItemBuffer;
			LoadSvc<T> nextLoadSvc;
			for(final String addr: keySet()) {
				try {
					nextDataItemBuffer = get(addr);
					nextLoadSvc = loadSvcMap.get(addr);
					nextDataItemBuffer.setConsumer(nextLoadSvc);
				} catch(final Exception e) {
					LogUtil.failure(
						LOG, Level.WARN, e,
						String.format(
							"Failed to set the consumer %s for remote data items buffer @ %s",
							consumer, addr
						)
					);
				}
			}
		} else {
			LOG.warn(
				LogUtil.ERR, "Attempted to set the invalid-type consumer: {}",
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
	public final void start() {
		DataItemBuffer<T> nextDataItemBuffer;
		for(final String addr: keySet()) {
			try {
				nextDataItemBuffer = get(addr);
				nextDataItemBuffer.start();
				LOG.debug(
					LogUtil.MSG, "Started producing from remote data items buffer @{}: \"{}\"",
					addr, nextDataItemBuffer.toString()
				);
			} catch(final Exception e) {
				LogUtil.failure(
					LOG, Level.WARN, e,
					String.format("Failed to start remote data items buffer @ %s", addr)
				);
			}
		}
	}
	//
	private final AtomicBoolean isInterruptedFlag = new AtomicBoolean(false);
	//
	@Override
	public final void interrupt() {
		isInterruptedFlag.set(true);
		DataItemBuffer<T> nextDataItemBuffer;
		for(final String addr: keySet()) {
			try {
				nextDataItemBuffer = get(addr);
				nextDataItemBuffer.interrupt();
				LOG.debug(
					LogUtil.MSG, "Interrupted producing from remote data items buffer @{}: \"{}\"",
					addr, nextDataItemBuffer.toString()
				);
			} catch(final Exception e) {
				LogUtil.failure(
					LOG, Level.WARN, e,
					String.format("Failed to interrupt remote data items buffer @ %s", addr)
				);
			}
		}
	}
	//
	@Override
	public final boolean isInterrupted() {
		return isInterruptedFlag.get();
	}
	//
	private final static class RemoteJoinTask
	implements Runnable {
		//
		private final String addr;
		private final DataItemBuffer dataItemBuffer;
		private final long timeOutMilliSec;
		//
		protected RemoteJoinTask(
			final String addr, final DataItemBuffer dataItemBuffer, final long timeOutMilliSec
		) {
			this.addr = addr;
			this.dataItemBuffer = dataItemBuffer;
			this.timeOutMilliSec = timeOutMilliSec;
		}
		//
		@Override
		public void run() {
			try {
				dataItemBuffer.join(timeOutMilliSec);
				LOG.debug(
					LogUtil.MSG,
					"Finished the remote data items buffer producing @{}: \"{}\"",
					addr, dataItemBuffer.toString()
				);
			} catch(final InterruptedException e) {
				LOG.debug(LogUtil.MSG, "Interrupted");
			} catch(final IOException e) {
				LogUtil.failure(
					LOG, Level.WARN, e,
					String.format(
						"Failed to await the remote data items buffer @%s", addr
					)
				);
			}
		}
	}
	//
	@Override
	public final void join()
	throws InterruptedException {
		join(Long.MAX_VALUE);
	}
	//
	@Override
	public final void join(final long milliSec)
	throws InterruptedException {
		//
		final ExecutorService remoteJoinExecutor = Executors.newFixedThreadPool(
			size(), new NamingWorkerFactory("itemBufferClientJoin")
		);
		//
		for(final String addr: keySet()) {
			try {
				final DataItemBuffer<T> nextDataItemBuffer = get(addr);
				remoteJoinExecutor.submit(new RemoteJoinTask(addr, nextDataItemBuffer, milliSec));
			} catch(final Exception e) {
				LogUtil.failure(
					LOG, Level.WARN, e,
					String.format("Failed to wait for remote data items buffer @ %s", addr)
				);
			}
		}
		//
		remoteJoinExecutor.shutdown();
		remoteJoinExecutor.awaitTermination(milliSec, TimeUnit.MILLISECONDS);
	}
}
