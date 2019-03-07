package com.emc.mongoose.base.svc;

import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.github.akurilov.commons.net.FixedPortRmiSocketFactory;
import java.io.IOException;
import java.lang.ref.WeakReference;
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
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.Level;

/** Created on 28.09.16. */
public abstract class ServiceUtil {

	private static final Map<Integer, WeakReference<Registry>> REGISTRY_MAP = new HashMap<>();
	private static final String RMI_SCHEME = "rmi";
	private static final String KEY_RMI_HOSTNAME = "java.rmi.server.hostname";
	private static final Map<String, WeakReference<Service>> SVC_MAP = new HashMap<>();

	public static void ensureRmiRegistryIsAvailableAt(final int port) throws RemoteException {
		synchronized (REGISTRY_MAP) {
			if (!REGISTRY_MAP.containsKey(port)) {
				try {
					REGISTRY_MAP.put(port, new WeakReference<>(LocateRegistry.createRegistry(port)));
				} catch (final RemoteException e) {
					REGISTRY_MAP.put(port, new WeakReference<>(LocateRegistry.getRegistry(port)));
				}
			}
		}
	}

	private static void ensureRmiUseFixedPort(final int port)
					throws IOException, IllegalStateException {
		final RMISocketFactory prevSocketFactory = RMISocketFactory.getSocketFactory();
		if (prevSocketFactory == null) {
			RMISocketFactory.setSocketFactory(new FixedPortRmiSocketFactory(port));
		} else if (prevSocketFactory instanceof FixedPortRmiSocketFactory) {
			((FixedPortRmiSocketFactory) prevSocketFactory).setFixedPort(port);
		} else {
			throw new IllegalStateException("Invalid RMI socket factory was set");
		}
	}

	public static URI getLocalSvcUri(final String svcName, final int port)
					throws URISyntaxException, SocketException {
		final String hostName = getAnyExternalHostAddress();
		return new URI(RMI_SCHEME, null, hostName, port, "/" + svcName, null, null);
	}

	private static URI getRemoteSvcUri(final String addr, final String svcName)
					throws URISyntaxException {
		final int port;
		final int portPos = addr.lastIndexOf(":");
		if (portPos < 0) {
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

	public static String getAnyExternalHostAddress() throws SocketException {

		String hostName = System.getProperty(KEY_RMI_HOSTNAME);
		if (hostName != null) {
			return hostName;
		}

		return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
						.filter(Objects::nonNull)
						.filter(
										netIface -> {
											try {
												return !netIface.isLoopback() && netIface.isUp();
											} catch (final SocketException e) {
												return false;
											}
										})
						.map(NetworkInterface::getInetAddresses)
						.map(
										inetAddrs -> Collections.list(inetAddrs).stream()
														.filter(inetAddr -> inetAddr instanceof Inet4Address)
														.map(InetAddress::getHostAddress)
														.findFirst())
						.filter(Optional::isPresent)
						.map(Optional::get)
						.findFirst()
						.orElse(InetAddress.getLoopbackAddress().getHostAddress());
	}

	public static boolean isLocalAddress(final String addrWithPort) {

		final String addr;
		final int portSepPos = addrWithPort.lastIndexOf(':');
		if (portSepPos == -1) {
			addr = addrWithPort;
		} else {
			addr = addrWithPort.substring(0, portSepPos);
		}

		try {
			return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
							.map(NetworkInterface::getInetAddresses)
							.anyMatch(
											ifaceAddrs -> Collections.list(ifaceAddrs).stream()
															.anyMatch(
																			inetAddress -> addr.equals(inetAddress.getCanonicalHostName())
																							|| addr.equals(inetAddress.getHostName())
																							|| addr.equals(inetAddress.getHostAddress())));
		} catch (final SocketException e) {
			LogUtil.trace(
							Loggers.ERR,
							Level.WARN,
							e,
							"Failed to list the network interfaces to find the local one");
		}

		return false;
	}

	public static String create(final Service svc, final int port)
					throws URISyntaxException, MalformedURLException, SocketException, RemoteException {
		String svcUri = null;
		synchronized (SVC_MAP) {
			// ensureRmiUseFixedPort(port);
			ensureRmiRegistryIsAvailableAt(port);
			UnicastRemoteObject.exportObject(svc, port);
			final String svcName = svc.name();
			svcUri = getLocalSvcUri(svcName, port).toString();
			if (!SVC_MAP.containsKey(svcName + ":" + port)) {
				Naming.rebind(svcUri, svc);
				SVC_MAP.put(svcName + ":" + port, new WeakReference<>(svc));
			} else {
				throw new AssertionError("Service already registered");
			}
		}
		return svcUri;
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S resolve(final String addr, final String name)
					throws NotBoundException, RemoteException, URISyntaxException, MalformedURLException {
		final String svcUri = getRemoteSvcUri(addr, name).toString();
		return (S) Naming.lookup(svcUri);
	}

	@SuppressWarnings("unchecked")
	public static <S extends Service> S resolve(final String addr, final int port, final String name)
					throws NotBoundException, IOException, URISyntaxException {
		final String svcUri = getRemoteSvcUri(addr, port, name).toString();
		return (S) Naming.lookup(svcUri);
	}

	public static String close(final Service svc) throws RemoteException, MalformedURLException {
		final String svcName = svc.name();
		String svcUri = null;
		try {
			UnicastRemoteObject.unexportObject(svc, true);
		} finally {
			try {
				svcUri = getLocalSvcUri(svcName, svc.registryPort()).toString();
				Naming.unbind(svcUri);
				synchronized (SVC_MAP) {
					if (null == SVC_MAP.remove(svcName + ":" + svc.registryPort())) {
						System.err.println("Failed to remove the service \"" + svcName + "\"");
					}
				}
			} catch (final NotBoundException | URISyntaxException | SocketException e) {
				LogUtil.trace(Loggers.ERR, Level.WARN, e, "Failed to close the RMI service");
			}
		}
		return svcUri;
	}

	public static void shutdown() {

		synchronized (SVC_MAP) {
			while (!SVC_MAP.isEmpty()) {
				final Service svc = SVC_MAP.values().iterator().next().get();
				if (svc != null) {
					try {
						System.out.println("Service closed: " + close(svc));
					} catch (final RemoteException | MalformedURLException e) {
						LogUtil.trace(Loggers.ERR, Level.WARN, e, "Failed to close the RMI service");
					}
				}
			}
			SVC_MAP.clear();
		}

		synchronized (REGISTRY_MAP) {
			REGISTRY_MAP.clear();
		}
	}
}
