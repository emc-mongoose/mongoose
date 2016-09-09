package com.emc.mongoose.storage.mock.impl.base;

import com.emc.mongoose.common.concurrent.TaskSequencer;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.StorageMockServer;
import com.emc.mongoose.storage.mock.impl.distribution.MDns;
import com.emc.mongoose.storage.mock.impl.distribution.UrlStrings;
import com.emc.mongoose.storage.mock.impl.http.Nagaina;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.emc.mongoose.storage.mock.impl.http.Nagaina.IDENTIFIER;
import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 Created on 06.09.16.
 */
public class BasicStorageMockClient<T extends MutableDataItemMock, O extends StorageMockServer<T>>
implements StorageMockClient<T, O> {

	private static final Logger LOG = LogManager.getLogger();

	private final JmDNS jmDns;
	private final Map<String, O> remoteNodes;
	private final ThreadPoolExecutor storagePoller;

	public BasicStorageMockClient(final JmDNS jmDns) {
		this.jmDns = jmDns;
		this.remoteNodes = new HashMap<>();
		this.storagePoller = new ThreadPoolExecutor(
			1, 1, 0, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(TaskSequencer.DEFAULT_TASK_QUEUE_SIZE_LIMIT)
		);
	}

	private void setStoragePollSize(final int size) {
		storagePoller.setCorePoolSize(size);
		storagePoller.setMaximumPoolSize(size);
	}

	@Override
	public void start() {
		jmDns.addServiceListener(MDns.Type.HTTP.toString(), this);
	}

	@Override
	public void close()
	throws IOException {
		jmDns.removeServiceListener(MDns.Type.HTTP.toString(), this);
	}

	@Override
	public void serviceAdded(final ServiceEvent event) {
		jmDns.requestServiceInfo(event.getType(), event.getName(), 10);
	}

	@Override
	public void serviceRemoved(final ServiceEvent event) {
		handleServiceEvent(event, remoteNodes::remove, "Node removed");
		setStoragePollSize(remoteNodes.size());
	}

	@Override
	public T readObject(
		final String containerName, final String id, final long offset, final long size
	) throws ExecutionException, InterruptedException {
		T object;
		final List<Future<T>> objFutures = new ArrayList<>(remoteNodes.size());
		for(final StorageMockServer<T> node : remoteNodes.values()) {
			objFutures.add(
				storagePoller.submit(
					() -> {
						return node.getObjectRemotely(containerName, id, offset, size);
					}
				)
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
		setStoragePollSize(remoteNodes.size());
	}

	private void printNodeList() {
		final StringJoiner joiner = new StringJoiner("\n");
		remoteNodes.keySet().forEach(joiner::add);
		LOG.info(Markers.MSG, "Detected nodes: \n" + joiner.toString());
	}

	private void handleServiceEvent(
		final ServiceEvent event, final Consumer<String> consumer, final String actionMsg) {
		final ServiceInfo eventInfo = event.getInfo();
		if (eventInfo.getQualifiedName().contains(IDENTIFIER)) {
			for (final InetAddress address: eventInfo.getInet4Addresses()) {
				try {
					if (!address.equals(jmDns.getInetAddress())) {
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
