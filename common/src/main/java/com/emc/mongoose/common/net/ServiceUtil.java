package com.emc.mongoose.common.net;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
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
import java.util.HashMap;
import java.util.Map;

/**
 Created on 28.09.16.
 */
public abstract class ServiceUtil {

	private static Int2ObjectMap<Registry> REGISTRY_MAP = new Int2ObjectOpenHashMap<>();
	private static final String RMI_SCHEME = "rmi";
	private static final String KEY_RMI_HOSTNAME = "java.rmi.server.hostname";
	private static final Map<String, Service> SVC_MAP = new HashMap<>();

	private static synchronized void ensureRmiRegistryIsAvailableAt(final int port)
	throws RemoteException {
		if(!REGISTRY_MAP.containsKey(port)) {
			try {
				REGISTRY_MAP.put(port, LocateRegistry.createRegistry(port));
			} catch(final RemoteException e) {
				REGISTRY_MAP.put(port, LocateRegistry.getRegistry(port));
			}
		}
	}

	public static URI getLocalSvcUri(final String svcName, final int port)
	throws URISyntaxException {
		final String hostName = getHostAddr();
		return new URI(RMI_SCHEME, null, hostName, port, "/" + svcName, null, null);
	}

	private static URI getRemoteSvcUri(final String addr, final String svcName)
	throws URISyntaxException {
		final int port;
		final int portPos = addr.lastIndexOf(":");
		if(portPos < 0) {
			throw new URISyntaxException(addr, "No port information in the address");
		} else {
			port = Integer.parseInt(addr.substring(portPos + 1));
		}
		return getRemoteSvcUri(addr.substring(0, portPos), port, svcName);
	}

	private static URI getRemoteSvcUri(final String addr, final int port, final String svcName)
	throws URISyntaxException {
		return new URI(RMI_SCHEME, null, addr, port, "/" + svcName, null, null);
	}

	public static String getHostAddr() {

		String hostName = System.getProperty(KEY_RMI_HOSTNAME);
		if(hostName != null) {
			return hostName;
		}

		InetAddress addr = null;
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

	public static String create(final Service svc, final int port) {
		try {
			ensureRmiRegistryIsAvailableAt(port);
			UnicastRemoteObject.exportObject(svc, 0);
			final String svcName = svc.getName();
			final String svcUri = getLocalSvcUri(svcName, port).toString();
			synchronized(SVC_MAP) {
				if(!SVC_MAP.containsKey(svcName + ":" + port)) {
					Naming.rebind(svcUri, svc);
					SVC_MAP.put(svcName + ":" + port, svc);
				} else {
					throw new AssertionError("Service already registered");
				}
			}
			return svcUri;
		} catch(final IOException | URISyntaxException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S resolve(final String addr, final String name)
	throws NotBoundException, IOException, URISyntaxException {
		final String svcUri = getRemoteSvcUri(addr, name).toString();
		return (S) Naming.lookup(svcUri);
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S resolve(
		final String addr, final int port, final String name
	) throws NotBoundException, IOException, URISyntaxException {
		final String svcUri = getRemoteSvcUri(addr, port, name).toString();
		return (S) Naming.lookup(svcUri);
	}

	public static String close(final Service svc)
	throws RemoteException, MalformedURLException {
		final String svcName = svc.getName();
		String svcUri = null;
		try {
			UnicastRemoteObject.unexportObject(svc, true);
		} finally {
			try {
				svcUri = getLocalSvcUri(svcName, svc.getRegistryPort()).toString();
				Naming.unbind(svcUri);
				synchronized(SVC_MAP) {
					if(null == SVC_MAP.remove(svcName + ":" + svc.getRegistryPort())) {
						System.err.println(
							"Failed to remove the service \"" + svcName + "\""
						);
					}
				}
			} catch(final NotBoundException | URISyntaxException e) {
				e.printStackTrace(System.err);
			}
		}
		return svcUri;
	}

	public static void shutdown() {

		synchronized(SVC_MAP) {
			for(final Service svc : SVC_MAP.values()) {
				try {
					System.out.println("Service closed: " + close(svc));
				} catch(final RemoteException | MalformedURLException e) {
					e.printStackTrace(System.err);
				}
			}
			SVC_MAP.clear();
		}

		REGISTRY_MAP.clear();
	}

}
