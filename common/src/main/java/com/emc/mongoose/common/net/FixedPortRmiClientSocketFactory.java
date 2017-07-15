package com.emc.mongoose.common.net;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

/**
 Created by andrey on 15.07.17.
 */
public final class FixedPortRmiClientSocketFactory
implements RMIClientSocketFactory, Serializable {

	private final int fixedPort;

	public FixedPortRmiClientSocketFactory(final int fixedPort) {
		this.fixedPort = fixedPort;
	}

	@Override
	public final Socket createSocket(final String host, final int port)
	throws IOException {
		return new Socket(host, fixedPort);
	}

	@Override
	public final int hashCode() {
		return fixedPort;
	}

	@Override
	public final boolean equals(final Object obj) {
		return (getClass() == obj.getClass() &&
			fixedPort == ((FixedPortRmiClientSocketFactory) obj).fixedPort);
	}
}
