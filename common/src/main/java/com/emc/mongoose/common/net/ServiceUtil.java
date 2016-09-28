package com.emc.mongoose.common.net;

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
					System.out.println("RMI registry created");
				} catch(final RemoteException e) {
					try {
						REGISTRY = LocateRegistry.getRegistry(REGISTRY_PORT);
						System.out.println("Reusing already existing RMI registry");
					} catch(final RemoteException ee) {
						System.err.println("Failed to obtain a RMI registry");
					}
				}
			}
		} finally {
			REGISTRY_LOCK.unlock();
		}
	}

	public static URI svcUri(final String svcName) {
		try {
			String hostName = System.getProperty(ServiceUtil.KEY_RMI_HOSTNAME);
			if (null != hostName) {
				hostName = getHostAddr();
			}
			return new URI(
				"rmi", null, hostName, REGISTRY_PORT, "/" + svcName, null, null
			);
		} catch(final URISyntaxException ignore) {
		}
		throw new IllegalArgumentException();
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
							System.out.println(
								"Resolved external interface \"" + nextNetIfaceName +
									"\" address: " + addr.getHostAddress()
							);
							break;
						}
					}
				} else {
					System.out.println(
						"Interface \"" + nextNetIfaceName +
							"\"is loopback or is not up, skipping"
					);
				}
			}
		} catch(final SocketException e) {
			System.err.println("Failed to get an external interface address");
		}
		if(addr == null) {
			System.err.println("No valid external interface have been found, " +
				"falling back to loopback");
			addr = InetAddress.getLoopbackAddress();
		}
		return addr.getHostAddress();
	}

	public static void create(final Service svc) {
		try {
			UnicastRemoteObject.exportObject(svc, 0);
			System.out.println("Exported service object successfully");
			final String svcName = svc.getName();
			final String svcUri = svcUri(svcName).toString();
			if(!SVCS.containsKey(svcUri)) {
				Naming.rebind(svcUri, svc);
				SVCS.put(svcName, svc);
				System.out.println("New service bound " + svcUri);
			} else {
				throw new IllegalStateException("Duplication of service name");
			}
		} catch(final RemoteException e) {
			try {
				System.err.println("Failed to export service object " + svc.getName());
			} catch(RemoteException ignore) {
			}
		} catch(final MalformedURLException e) {
			System.err.println("Invalid service URL");
		}
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S getSvc(final String name) {
		final String svcUri = svcUri(name).toString();
		try {
			return (S) Naming.lookup(svcUri);
		} catch(final NotBoundException e) {
			System.err.println("No service bound with URL " + svcUri);
		} catch(final MalformedURLException e) {
			System.err.println("Invalid service URL " + svcUri);
		} catch(final RemoteException e) {
			System.err.println("Failed to get service");
		}
		return null;
	}

	public static void close(final Service svc)
	throws RemoteException {
		try {
			UnicastRemoteObject.unexportObject(svc, true);
			System.out.println("Unexported service object");
		} catch(final NoSuchObjectException e) {
			System.err.println("Failed to unexport service object");
		}

		final String svcUri = svcUri(svc.getName()).toString();
		try {
			Naming.unbind(svcUri);
			SVCS.remove(svcUri);
			System.out.println("Removed service: " + svcUri);
		} catch(final NotBoundException e) {
			System.err.println("Service not bound");
		} catch(final MalformedURLException e) {
			System.err.println("Invalid service URL " + svcUri);
		}
	}

	public static void shutdown() {
		for(final Service svc : SVCS.values()) {
			try {
				close(svc);
			} catch(final RemoteException e) {
				System.err.println("Failed to shutdown service");
			}
		}
	}

}
