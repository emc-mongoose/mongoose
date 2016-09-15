package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.concurrent.AnyNotNullSharedFutureTaskBase;
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
import java.util.StringJoiner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import static com.emc.mongoose.storage.mock.impl.http.Nagaina.IDENTIFIER;
import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 Created on 06.09.16.
 */
public final class BasicStorageMockClient<T extends MutableDataItemMock, O extends StorageMockServer<T>>
extends ThreadPoolExecutor
implements StorageMockClient<T> {

	private static final Logger LOG = LogManager.getLogger();

	private final ContentSource contentSrc;
	private final JmDNS jmDns;
	private final ConcurrentMap<String, O> remoteNodeMap = new ConcurrentHashMap<>();

	public BasicStorageMockClient(final ContentSource contentSrc, final JmDNS jmDns) {
		super(
			ThreadUtil.getAvailableConcurrencyLevel(), ThreadUtil.getAvailableConcurrencyLevel(),
			0, TimeUnit.DAYS,
			new ArrayBlockingQueue<>(TaskSequencer.DEFAULT_TASK_QUEUE_SIZE_LIMIT),
			new RejectedExecutionHandler() {
				@Override
				public final void rejectedExecution(final Runnable r, final ThreadPoolExecutor e) {
					LOG.error("Task {} rejected", r.toString());
				}
			}
		);
		this.contentSrc = contentSrc;
		this.jmDns = jmDns;
	}
	
	@Override
	public final Future<T> submit(final Runnable task) {
		final RunnableFuture<T> rf = (RunnableFuture<T>) task;
		execute(rf);
		return rf;
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
		final Collection<O> remoteNodes = remoteNodeMap.values();
		final CountDownLatch sharedCountDown = new CountDownLatch(remoteNodes.size());
		final AtomicReference<T> resultRef = new AtomicReference<>(null);
		for(final O node : remoteNodes) {
			submit(
				new GetRemoteObjectTask<>(
					resultRef, sharedCountDown, node, containerName, id, offset, size
				)
			);
		}
		T result;
		do {
			result = resultRef.get();
			if(result == null) {
				LockSupport.parkNanos(1);
			} else {
				result.setContentSrc(contentSrc);
				break;
			}
		} while(sharedCountDown.getCount() > 0);
		return result;
	}
	
	@Override
	public void start() {
		jmDns.addServiceListener(MDns.Type.HTTP.toString(), this);
	}

	@Override
	public void close()
	throws IOException {
		shutdownNow();
		jmDns.removeServiceListener(MDns.Type.HTTP.toString(), this);
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
						"rmi", null, hostAddress, REGISTRY_PORT, "/" + IDENTIFIER, null, null
					);
					final O mock = (O) Naming.lookup(rmiUrl.toString());
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
		if(eventInfo.getQualifiedName().contains(IDENTIFIER)) {
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
