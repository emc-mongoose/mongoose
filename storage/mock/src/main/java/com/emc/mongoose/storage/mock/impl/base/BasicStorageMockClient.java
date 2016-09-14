package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.concurrent.FutureTaskBase;
import com.emc.mongoose.common.concurrent.TaskSequencer;
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.StorageMockServer;
import com.emc.mongoose.storage.mock.api.exception.ContainerMockException;
import com.emc.mongoose.storage.mock.impl.distribution.MDns;
import com.emc.mongoose.storage.mock.impl.distribution.UrlStrings;
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
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

	private final JmDNS jmDns;
	private final Map<String, O> remoteNodes;

	public BasicStorageMockClient(final JmDNS jmDns) {
		super(ThreadUtil.getAvailableConcurrencyLevel(), ThreadUtil.getAvailableConcurrencyLevel(),
			0, TimeUnit.DAYS,
			new ArrayBlockingQueue<>(TaskSequencer.DEFAULT_TASK_QUEUE_SIZE_LIMIT),
			new RejectedExecutionHandler() {
				@Override
				public final void rejectedExecution(final Runnable r, final ThreadPoolExecutor e) {
					LOG.error("Task {} rejected", r.toString());
				}
			}
		);
		this.jmDns = jmDns;
		this.remoteNodes = new HashMap<>();
		final int concurrencyLevel = ThreadUtil.getAvailableConcurrencyLevel();
	}
	
	@Override
	public final Future<T> submit(final Runnable task) {
		final RunnableFuture<T> rf = (RunnableFuture<T>) task;
		execute(rf);
		return rf;
	}
	
	private final static class GetRemoteObjectTask<T extends MutableDataItemMock>
	extends FutureTaskBase<T> {
		
		private final StorageMockServer<T> node;
		private final String containerName;
		private final String id;
		private final long offset;
		private final long size;
		
		public GetRemoteObjectTask(
			final StorageMockServer<T> node, final String containerName, final String id,
			final long offset, final long size
		) {
			this.node = node;
			this.containerName = containerName;
			this.id = id;
			this.offset = offset;
			this.size = size;
		}
		
		@Override
		public final void run() {
			try {
				set(node.getObjectRemotely(containerName, id, offset, size));
			} catch(final ContainerMockException | RemoteException e) {
				setException(e);
			}
		}
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
		handleServiceEvent(event, remoteNodes::remove, "Node removed");
	}

	@Override
	public T getObject(
		final String containerName, final String id, final long offset, final long size
	) throws ExecutionException, InterruptedException {
		T object;
		final List<Future<T>> objFutures = new ArrayList<>(remoteNodes.size());
		for(final StorageMockServer<T> node : remoteNodes.values()) {
			objFutures.add(
				submit(new GetRemoteObjectTask<>(node, containerName, id, offset, size))
			);
		}
		for (final Future<T> objFuture: objFutures) {
			object = objFuture.get();
			if (object != null) {
				return object;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void serviceResolved(final ServiceEvent event) {
		handleServiceEvent(
			event,
			(hostAddress) -> {
				final String rmiUrl =
					UrlStrings.get("rmi", hostAddress, REGISTRY_PORT, IDENTIFIER);
				try {
					final O mock = (O) Naming.lookup(rmiUrl);
					remoteNodes.put(hostAddress, mock);
				} catch(final NotBoundException | MalformedURLException | RemoteException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to lookup node");
				}
			},
			"Node added"
		);
	}

	private void printNodeList() {
		final StringJoiner joiner = new StringJoiner("\n");
		remoteNodes.keySet().forEach(joiner::add);
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
