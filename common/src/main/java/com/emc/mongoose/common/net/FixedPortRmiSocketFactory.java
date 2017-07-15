package com.emc.mongoose.common.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;

/**
 Created by andrey on 15.07.17.
 */
public final class FixedPortRmiSocketFactory
extends RMISocketFactory
implements RMIServerSocketFactory {

	public final int fixedPort;

	public FixedPortRmiSocketFactory(final int fixedPort) {
		this.fixedPort = fixedPort;
	}

	@Override
	public final Socket createSocket(final String host, final int port)
	throws IOException {
		return new Socket(host, fixedPort);
	}

	@Override
	public final ServerSocket createServerSocket(final int port)
	throws IOException {
		return new ServerSocket(fixedPort);
	}
}
