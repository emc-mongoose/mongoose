package com.emc.mongoose.common.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
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
	private static final String RMI_SCHEME = "rmi";
	private static final String KEY_RMI_HOSTNAME = "java.rmi.server.hostname";
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
						ee.printStackTrace(System.err);
					}
				}
			}
		} finally {
			REGISTRY_LOCK.unlock();
		}
	}

	public static URI getLocalSvcUri(final String svcName)
	throws URISyntaxException {
		String hostName = System.getProperty(KEY_RMI_HOSTNAME);
		if(hostName == null) {
			hostName = getHostAddr();
		}
		return new URI(RMI_SCHEME, null, hostName, REGISTRY_PORT, "/" + svcName, null, null);
	}

	public static URI getRemoteSvcUri(final String addr, final String svcName)
	throws URISyntaxException {
		return new URI(RMI_SCHEME, null, addr, REGISTRY_PORT, "/" + svcName, null, null);
	}

	public static String getHostAddr() {
		InetAddress addr = null;
		//
		try {
			final Enumeration<NetworkInterface> netIfaces = NetworkInterface.getNetworkInterfaces();
			NetworkInterface nextNetIface;
			String nextNetIfaceName;
			while(netIfaces.hasMoreElements()) {
				nextNetIface = netIfaces.nextElement();
				nextNetIfaceName = nextNetIface.getDisplayName();
				if(!nextNetIface.isLoopback() && nextNetIface.isUp()) {
					final Enumeration<InetAddress> addrs = nextNetIface.getInetAddresses();
					while(addrs.hasMoreElements()) {
						addr = addrs.nextElement();
						if(Inet4Address.class.isInstance(addr)) {
							break;
						}
					}
				}
			}
		} catch(final SocketException e) {
			e.printStackTrace(System.err);
		}
		if(addr == null) {
			addr = InetAddress.getLoopbackAddress();
		}
		return addr.getHostAddress();
	}

	public static String create(final Service svc) {
		try {
			UnicastRemoteObject.exportObject(svc, 0);
			final String svcName = svc.getName();
			final String svcUri = getLocalSvcUri(svcName).toString();
			if(!SVC_MAP.containsKey(svcUri)) {
				Naming.rebind(svcUri, svc);
				SVC_MAP.put(svcName, svc);
			} else {
				throw new IllegalStateException("Service already registered");
			}
			return svcUri;
		} catch(final IOException | URISyntaxException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S resolve(final String addr, final String name) {
		try {
			final String svcUri = getRemoteSvcUri(addr, name).toString();
			return (S) Naming.lookup(svcUri);
		} catch(final NotBoundException | IOException | URISyntaxException e) {
			e.printStackTrace(System.err);
		}
		return null;
	}

	public static void close(final Service svc)
	throws RemoteException {
		try {
			UnicastRemoteObject.unexportObject(svc, true);
		} catch(final NoSuchObjectException e) {
			e.printStackTrace(System.err);
		} finally {
			try {
				final String svcUri = getLocalSvcUri(svc.getName()).toString();
				Naming.unbind(svcUri);
				SVC_MAP.remove(svcUri);
			} catch(final NotBoundException | MalformedURLException | URISyntaxException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	public static void shutdown() {
		for(final Service svc : SVC_MAP.values()) {
			try {
				close(svc);
			} catch(final RemoteException e) {
				e.printStackTrace(System.err);
			}
		}
	}

}
