package com.emc.mongoose.storage.mock.impl.distribution;

import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.RemoteStorageMock;
import com.emc.mongoose.storage.mock.api.RemoteStorageMockListener;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.Closeable;
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
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 Created on 23.08.16.
 */
public class BasicRemoteStorageMockListener
implements RemoteStorageMockListener<MutableDataItemMock, RemoteStorageMock<MutableDataItemMock>> {

	private static final Logger LOG = LogManager.getLogger();

	private final String id;
	private final JmDNS jmDns;
	private final String type;
	private final Map<String, RemoteStorageMock<MutableDataItemMock>> remoteNodes;

	public BasicRemoteStorageMockListener(final String id, final JmDNS jmDns, final MDns.Type type)
	throws IOException {
		this.id = id;
		this.jmDns = jmDns;
		this.type = type.toString();
		this.remoteNodes = new HashMap<>();
	}

	@Override
	public void serviceAdded(final ServiceEvent event) {
		jmDns.requestServiceInfo(event.getType(), event.getName(), 10);
	}

	@Override
	public void serviceRemoved(final ServiceEvent event) {
		handleServiceEvent(event, remoteNodes::remove, "Node removed");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void serviceResolved(final ServiceEvent event) {
		handleServiceEvent(
			event,
			(hostAddress) -> {
				final String rmiUrl =
					UrlStrings.get("rmi", hostAddress, REGISTRY_PORT, id);
				try {
					final RemoteStorageMock<MutableDataItemMock> mock =
						(RemoteStorageMock<MutableDataItemMock>) Naming.lookup(rmiUrl);
					remoteNodes.put(hostAddress, mock);
				} catch(final NotBoundException | MalformedURLException | RemoteException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to lookup node");
				}
			},
			"Node added"
		);
	}

	private void handleServiceEvent(
		final ServiceEvent event, final Consumer<String> consumer, final String actionMsg) {
		final ServiceInfo eventInfo = event.getInfo();
		if (eventInfo.getQualifiedName().contains(id)) {
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

	public Collection<RemoteStorageMock<MutableDataItemMock>> getNodes() {
		return remoteNodes.values();
	}

	public void printNodeList() {
		final StringJoiner joiner = new StringJoiner("\n");
		remoteNodes.keySet().forEach(joiner::add);
		LOG.info(Markers.MSG, "Detected nodes: \n" + joiner.toString());
	}

	public void open() {
		jmDns.addServiceListener(type, this);
	}

	@Override
	public void close()
	throws IOException {
		jmDns.removeServiceListener(type, this);
	}
}
