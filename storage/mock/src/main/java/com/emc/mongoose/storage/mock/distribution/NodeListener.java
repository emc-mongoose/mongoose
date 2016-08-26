package com.emc.mongoose.storage.mock.distribution;

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
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 Created on 23.08.16.
 */
public class NodeListener
	implements ServiceListener, Closeable {

	private static final String NODE_IDENTIFIER = "nagaina";
	private static final Logger LOG = LogManager.getLogger();

	private final JmDNS jmDns;
	private final String type;
	private final List<InetAddress> nodesAddresses;

	public NodeListener(final JmDNS jmDns, final MDns.Type type)
	throws IOException {
		this.jmDns = jmDns;
		this.type = type.toString();
		this.nodesAddresses = new ArrayList<>();
	}

	@Override
	public void serviceAdded(final ServiceEvent event) {
		jmDns.requestServiceInfo(event.getType(), event.getName(), 10);
	}

	@Override
	public void serviceRemoved(final ServiceEvent event) {
		handleServiceEvent(event, nodesAddresses::remove, "Node removed");
	}

	@Override
	public void serviceResolved(final ServiceEvent event) {
		handleServiceEvent(event, nodesAddresses::add, "Node added");
	}

	private void handleServiceEvent(
		final ServiceEvent event, final Consumer<InetAddress> consumer, final String actionMsg) {
		final ServiceInfo eventInfo = event.getInfo();
		if (eventInfo.getQualifiedName().contains(NODE_IDENTIFIER)) {
			for (final InetAddress address: eventInfo.getInet4Addresses()) {
				try {
					if (!address.equals(jmDns.getInetAddress())) {
						consumer.accept(address);
						LOG.info(Markers.MSG, actionMsg + ":" + event.getName());
						printNodeList();
					}
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to get own host address");
				}
			}
		}
	}

	public void printNodeList() {
		final String nodeListString = nodesAddresses.stream().map(InetAddress:: getHostAddress).collect(
			Collectors.joining("\n"));
		LOG.info(Markers.MSG, "Detected nodes: \n" + nodeListString);
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
