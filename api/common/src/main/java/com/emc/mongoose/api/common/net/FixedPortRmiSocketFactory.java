package com.emc.mongoose.api.common.net;

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

	public int fixedPort;

	public FixedPortRmiSocketFactory(final int fixedPort) {
		this.fixedPort = fixedPort;
	}

	public final void setFixedPort(final int fixedPort) {
		this.fixedPort = fixedPort;
	}

	@Override
	public final Socket createSocket(final String host, final int port)
	throws IOException {
		System.out.println("New socket @ port # " + port);
		return new Socket(host, port);
	}

	@Override
	public final ServerSocket createServerSocket(final int port)
	throws IOException {
		System.out.println("New server socket @ port # " + port);
		return new ServerSocket(port);
	}
}
