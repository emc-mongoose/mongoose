package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.concurrent.AnyNotNullSharedFutureTaskBase;
import com.emc.mongoose.common.concurrent.DaemonBase;
import com.emc.mongoose.common.concurrent.TaskSequencer;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.StorageMockServer;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.impl.remote.MDns;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import static com.emc.mongoose.storage.mock.impl.http.Nagaina.SVC_NAME;
import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 Created on 06.09.16.
 */
public final class BasicStorageMockClient<T extends MutableDataItemMock>
extends DaemonBase
implements StorageMockClient<T> {

	private static final Logger LOG = LogManager.getLogger();

	private final ContentSource contentSrc;
	private final JmDNS jmDns;
	private final Map<String, StorageMockServer<T>>
		remoteNodeMap = new ConcurrentHashMap<>();
	private final ExecutorService executor;

	public BasicStorageMockClient(final ContentSource contentSrc, final JmDNS jmDns) {
		this.executor = new ThreadPoolExecutor(
			ThreadUtil.getAvailableConcurrencyLevel(), ThreadUtil.getAvailableConcurrencyLevel(),
			0, TimeUnit.DAYS, new ArrayBlockingQueue<>(TaskSequencer.DEFAULT_TASK_QUEUE_SIZE_LIMIT),
			(r, e) -> LOG.error("Task {} rejected", r.toString())
		) {
			@Override
			public final Future<T> submit(final Runnable task) {
				final RunnableFuture<T> rf = (RunnableFuture<T>) task;
				execute(rf);
				return rf;
			}
		};
		this.contentSrc = contentSrc;
		this.jmDns = jmDns;
	}
	
	private final static class GetRemoteObjectTask<T extends MutableDataItemMock>
	extends AnyNotNullSharedFutureTaskBase<T> {
		
		private final StorageMockServer<T> node;
		private final String containerName;
		private final String id;
		private final long offset;
		private final long size;
		
		public GetRemoteObjectTask(
			final AtomicReference<T> resultRef, final CountDownLatch sharedLatch,
			final StorageMockServer<T> node, final String containerName, final String id,
			final long offset, final long size
		) {
			super(resultRef, sharedLatch);
			this.node = node;
			this.containerName = containerName;
			this.id = id;
			this.offset = offset;
			this.size = size;
		}
		
		@Override
		public final void run() {
			try {
				final T remoteObject = node.getObjectRemotely(containerName, id, offset, size);
				set(remoteObject);
			} catch(final ContainerMockException | RemoteException e) {
				setException(e);
			}
		}
	}
	
	@Override
	public T getObject(
		final String containerName, final String id, final long offset, final long size
	) throws ExecutionException, InterruptedException {
		final Collection<StorageMockServer<T>> remoteNodes = remoteNodeMap.values();
		final CountDownLatch sharedCountDown = new CountDownLatch(remoteNodes.size());
		final AtomicReference<T> resultRef = new AtomicReference<>(null);
		for(final StorageMockServer<T> node : remoteNodes) {
			executor.submit(
				new GetRemoteObjectTask<>(
					resultRef, sharedCountDown, node, containerName, id, offset, size
				)
			);
		}
		T result;
		while(null == (result = resultRef.get()) && sharedCountDown.getCount() > 0) {
			LockSupport.parkNanos(1);
		}
		if(result != null) {
			result.setContentSrc(contentSrc);
		}
		return result;
	}
	
	@Override
	protected void doStart() {
		jmDns.addServiceListener(MDns.Type.HTTP.toString(), this);
	}
	
	@Override
	protected void doShutdown() {
		executor.shutdown();
	}
	
	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return executor.awaitTermination(timeout, timeUnit);
	}
	
	@Override
	protected void doInterrupt() {
		executor.shutdownNow();
		jmDns.removeServiceListener(MDns.Type.HTTP.toString(), this);
	}
	
	@Override
	protected void doClose()
	throws IOException {
		remoteNodeMap.clear();
	}

	@Override
	public void serviceAdded(final ServiceEvent event) {
		jmDns.requestServiceInfo(event.getType(), event.getName(), 10);
	}

	@Override
	public void serviceRemoved(final ServiceEvent event) {
		handleServiceEvent(event, remoteNodeMap::remove, "Node removed");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void serviceResolved(final ServiceEvent event) {
		final Consumer<String> c = new Consumer<String>() {
			@Override
			public final void accept(final String hostAddress) {
				try {
					final URI rmiUrl = new URI(
						"rmi", null, hostAddress, REGISTRY_PORT, "/" + SVC_NAME, null, null
					);
					final StorageMockServer<T> mock = (StorageMockServer<T>) Naming.lookup(
						rmiUrl.toString()
					);
					remoteNodeMap.putIfAbsent(hostAddress, mock);
				} catch(final NotBoundException | MalformedURLException | RemoteException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to lookup node");
				} catch(final URISyntaxException e) {
					LOG.debug(Markers.ERR, "RMI URL syntax error {}", e);
				}
			}
		};
		handleServiceEvent(event, c, "Node added");
	}

	private void printNodeList() {
		final StringJoiner joiner = new StringJoiner("\n");
		remoteNodeMap.keySet().forEach(joiner::add);
		LOG.info(Markers.MSG, "Detected nodes: \n" + joiner.toString());
	}

	private void handleServiceEvent(
		final ServiceEvent event, final Consumer<String> consumer, final String actionMsg
	) {
		final ServiceInfo eventInfo = event.getInfo();
		if(eventInfo.getQualifiedName().contains(SVC_NAME)) {
			for (final InetAddress address: eventInfo.getInet4Addresses()) {
				try {
					if(!address.equals(jmDns.getInetAddress())) {
						consumer.accept(address.getHostAddress());
						LOG.info(Markers.MSG, actionMsg + ":" + event.getName());
						printNodeList();
					}
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to get own host address");
				}
			}
		}
	}

}
