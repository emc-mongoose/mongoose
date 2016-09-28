package com.emc.mongoose.common.net;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 Created on 28.09.16.
 */
public abstract class ServiceUtil {


	private static Registry REGISTRY = null;
	private static final String KEY_RMI_HOSTNAME = "java.rmi.server.hostname";
	private static final Lock REGISTRY_LOCK = new ReentrantLock();
	private static final Map<String, Service> SVCS = new ConcurrentHashMap<>();

	static {
		rmiRegistryInit();
	}

	private static void rmiRegistryInit() {
		REGISTRY_LOCK.lock();
		try {
			if(REGISTRY == null) {
				try {
					REGISTRY = LocateRegistry.createRegistry(REGISTRY_PORT);
				} catch(final RemoteException e) {
					try {
						REGISTRY = LocateRegistry.getRegistry(REGISTRY_PORT);
					} catch(final RemoteException ee) {
						throw new IllegalStateException(ee);
					}
				}
			}
		} finally {
			REGISTRY_LOCK.unlock();
		}
	}

	public static URI svcUri(final String svcName)
	throws SocketException, OmgLookAtMyConsoleException, OmgDoesNotPerformException {
		String hostName = System.getProperty(ServiceUtil.KEY_RMI_HOSTNAME);
		if (null != hostName) {
			hostName = NetUtil.getHostAddrString();
		}
		return svcUri(hostName, svcName);
	}

	private static URI svcUri(final String hostName, final String svcName)
	throws SocketException, OmgLookAtMyConsoleException, OmgDoesNotPerformException {
		try {
			return new URI(
				"rmi", null, hostName, REGISTRY_PORT, "/" + svcName, null, null
			);
		} catch(final URISyntaxException ignore) {
		}
		throw new IllegalArgumentException();
	}


	public static void create(final Service svc)
	throws RemoteException, MalformedURLException, SocketException,
		OmgLookAtMyConsoleException, OmgDoesNotPerformException {
		UnicastRemoteObject.exportObject(svc, 0);
		final String svcName = svc.getName();
		final String svcUri = svcUri(svcName).toString();
		if(!SVCS.containsKey(svcUri)) {
			Naming.rebind(svcUri, svc);
			SVCS.put(svcName, svc);
		} else {
			throw new IllegalStateException("Duplication of service name");
		}
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S getSvc(final String name)
	throws RemoteException, NotBoundException, MalformedURLException, SocketException,
		OmgLookAtMyConsoleException, OmgDoesNotPerformException {
		final String svcUri = svcUri(name).toString();
		return (S) Naming.lookup(svcUri);
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S getSvc(final String host, final String name)
	throws RemoteException, NotBoundException, MalformedURLException, SocketException,
		OmgLookAtMyConsoleException, OmgDoesNotPerformException {
		final String svcUri = svcUri(host, name).toString();
		return (S) Naming.lookup(svcUri);
	}

	public static void close(final Service svc)
	throws RemoteException, MalformedURLException, NotBoundException, SocketException,
		OmgLookAtMyConsoleException, OmgDoesNotPerformException {
		UnicastRemoteObject.unexportObject(svc, true);
		final String svcUri = svcUri(svc.getName()).toString();
		Naming.unbind(svcUri);
		SVCS.remove(svcUri);
	}

	public static void shutdown()
	throws RemoteException, NotBoundException, MalformedURLException, SocketException,
		OmgLookAtMyConsoleException, OmgDoesNotPerformException {
		for(final Service svc : SVCS.values()) {
			close(svc);
		}
	}

}
