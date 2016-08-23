package com.emc.mongoose.storage.mock.distribution;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.Closeable;
import java.io.IOException;

/**
 Created on 23.08.16.
 */
public class NagainaListener
	implements ServiceListener, Closeable {

	private final JmDNS jmDns;
	private final String type;
	private boolean nagainaDetector = false;

	public NagainaListener(final JmDNS jmDns, final MDns.Type type)
	throws IOException {
		this.jmDns = jmDns;
		this.type = type.toString();
	}

	@Override
	public void serviceAdded(final ServiceEvent event) {
		jmDns.requestServiceInfo(event.getType(), event.getName(), 1);
	}

	@Override
	public void serviceRemoved(final ServiceEvent event) {
		System.out.println("Service removed: " + event.getName());
	}

	@Override
	public void serviceResolved(final ServiceEvent event) {
		final ServiceInfo eventInfo = event.getInfo();
		System.out.println("Service resolved: " + eventInfo.getQualifiedName() +
			";\nport: " + eventInfo.getPort());
		if (eventInfo.getQualifiedName().contains("nagaina")) {
			nagainaDetector = true;
		}
	}

	public boolean isNagainaDetected() {
		return nagainaDetector;
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
