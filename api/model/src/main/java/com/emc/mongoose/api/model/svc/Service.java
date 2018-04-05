package com.emc.mongoose.api.model.svc;

import com.github.akurilov.concurrent.AsyncRunnable;

import sun.rmi.server.UnicastRef;
import sun.rmi.transport.Channel;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObjectInvocationHandler;

import static java.lang.reflect.Proxy.getInvocationHandler;

/**
 Created by kurila on 07.05.14.
 A remote service which has a name for resolution by URI.
 */
public interface Service
extends AsyncRunnable, Remote {

	int registryPort()
	throws RemoteException;

	String name()
	throws RemoteException;

	static String address(final Service svc)
	throws RemoteException {
		final RemoteObjectInvocationHandler
			h = (RemoteObjectInvocationHandler) getInvocationHandler(svc);
		final LiveRef ref = ((UnicastRef) h.getRef()).getLiveRef();
		final Channel channel = ref.getChannel();
		final TCPEndpoint endpoint = (TCPEndpoint) channel.getEndpoint();
		return endpoint.getHost() + ":" + endpoint.getPort();
	}
}
