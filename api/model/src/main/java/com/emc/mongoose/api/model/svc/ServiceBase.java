package com.emc.mongoose.api.model.svc;

import com.emc.mongoose.api.model.concurrent.AsyncRunnableBase;
import sun.rmi.server.UnicastRef;
import sun.rmi.transport.Channel;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObjectInvocationHandler;

import static java.lang.reflect.Proxy.getInvocationHandler;

public abstract class ServiceBase
extends AsyncRunnableBase
implements Service {

	protected final int port;

	protected ServiceBase(final int port) {
		this.port = port;
	}

	@Override
	public final int registryPort() {
		return port;
	}

	@Override
	protected void doStart() {
		ServiceUtil.create(this, port);
	}

	@Override
	protected void doStop()
	throws RemoteException {
		try {
			ServiceUtil.close(this);
		} catch(final MalformedURLException e) {
			throw new RemoteException("Failed to stop the service " + name(), e);
		}
	}
}
