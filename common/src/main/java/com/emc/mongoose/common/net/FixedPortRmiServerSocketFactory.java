package com.emc.mongoose.common.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

/**
 Created by andrey on 15.07.17.
 */
public final class FixedPortRmiServerSocketFactory
implements RMIServerSocketFactory {

	private final int fixedPort;

	public FixedPortRmiServerSocketFactory(final int fixedPort) {
		this.fixedPort = fixedPort;
	}

	@Override
	public final ServerSocket createServerSocket(final int port)
	throws IOException {
		return new ServerSocket(fixedPort);
	}

	@Override
	public final int hashCode() {
		return fixedPort;
	}

	@Override
	public final boolean equals(final Object obj) {
		return (getClass() == obj.getClass() &&
			fixedPort == ((FixedPortRmiServerSocketFactory) obj).fixedPort);
	}
}
