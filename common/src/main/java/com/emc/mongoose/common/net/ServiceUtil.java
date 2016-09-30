package com.emc.mongoose.common.net;

import com.emc.mongoose.common.exception.DanShootHisFootException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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
	private static final String RMI_HOSTNAME = System.getProperty("java.rmi.server.hostname");
	private static final Lock REGISTRY_LOCK = new ReentrantLock();
	private static final Map<String, Service> SVC_MAP = new ConcurrentHashMap<>();

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
	throws DanShootHisFootException {
		String hostName;
		if (null != RMI_HOSTNAME) {
			hostName = RMI_HOSTNAME;
		} else {
			hostName = NetUtil.getHostAddrString();
		}
		return svcUri(hostName, svcName);
	}

	private static URI svcUri(final String hostName, final String svcName) {
		try {
			return new URI("rmi", null, hostName, REGISTRY_PORT, "/" + svcName, null, null);
		} catch(final URISyntaxException ignore) {
		}
		throw new IllegalArgumentException();
	}


	public static void create(final Service svc)
	throws DanShootHisFootException, IOException {
		UnicastRemoteObject.exportObject(svc, 0);
		final String svcName = svc.getName();
		final String svcUri = svcUri(svcName).toString();
		if (null == SVC_MAP.putIfAbsent(svcName, svc)) {
			Naming.rebind(svcUri, svc);
		} else {
			throw new IllegalStateException("Duplication of service name");
		}
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S getSvc(final String name)
	throws DanShootHisFootException, IOException, NotBoundException {
		final String svcUri = svcUri(name).toString();
		return (S) Naming.lookup(svcUri);
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S getSvc(final String host, final String name)
	throws IOException, NotBoundException {
		final String svcUri = svcUri(host, name).toString();
		return (S) Naming.lookup(svcUri);
	}

	public static void close(final Service svc)
	throws DanShootHisFootException, IOException, NotBoundException {
		UnicastRemoteObject.unexportObject(svc, true);
		final String svcUri = svcUri(svc.getName()).toString();
		Naming.unbind(svcUri);
		SVC_MAP.remove(svcUri);
	}

	public static void shutdown()
	throws DanShootHisFootException, IOException, NotBoundException {
		for(final Service svc : SVC_MAP.values()) {
			close(svc);
		}
	}

}
