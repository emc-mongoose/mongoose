package com.emc.mongoose.common.net;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteStub;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 Created by kurila on 05.05.14.
 */
public final class ServiceUtils {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String JAVA_RMI_SERVER_HOSTNAME = "java.rmi.server.hostname";
	public final static int PORT_RMI_CONTROL;
	static {
		int tmpPort = Registry.REGISTRY_PORT;
		try {
			tmpPort = RunTimeConfig.getContext().getRemotePortControl();
		} catch(final Exception e) {
			LogUtil.failure(
				LOG, Level.WARN, e,
				String.format(
					"Failed to take remote control port value, will use the default value \"%d\"",
					tmpPort
				)
			);
		} finally {
			PORT_RMI_CONTROL = tmpPort;
		}
	}
	//
	private final static File JAR_SELF;
	static {
		File jarSelf = null;
		try {
			jarSelf = new File(
				ServiceUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI()
			);
		} catch(final URISyntaxException e) {
			LogUtil.failure(LOG, Level.WARN, e, "Determining the launcher path failure");
		}
		JAR_SELF = jarSelf;

	}
	//
	static {
		Runtime.getRuntime().addShutdownHook(
			new Thread("remoteSvcShutDownHook") {
				@Override
				public final void run() {
					shutdown();
				}
			}
		);
	}
	//
	private final static Map<String, Service> SVC_MAP;
	//private final static Registry REGISTRY;
	static {
		// set up security manager
		if(System.getSecurityManager() == null) {
			final SecurityManager sm = new SecurityManager();
			LOG.trace(LogUtil.MSG, "New security manager instance created");
			System.setSecurityManager(sm);
		}
		//
		SVC_MAP = new ConcurrentHashMap<>();
		// create or use existing registry
		//Registry registry = null;
		try {
			/*registry = */LocateRegistry.createRegistry(PORT_RMI_CONTROL);
			LOG.debug(LogUtil.MSG, "RMI registry created");
		} catch(final RemoteException e) {
			try {
				/*registry = */LocateRegistry.getRegistry(PORT_RMI_CONTROL);
				LOG.info(LogUtil.MSG, "Reusing already existing RMI registry");
			} catch(final RemoteException ee) {
				LOG.fatal(LogUtil.ERR, "Failed to obtain RMI registry", ee);
			}
		}
		//REGISTRY = registry;
	}
	//
	public static String getHostAddr() {
		InetAddress addr = null;
		//
		try {
			final Enumeration<NetworkInterface> netIfaces = NetworkInterface.getNetworkInterfaces();
			NetworkInterface nextNetIface;
			while(netIfaces.hasMoreElements()) {
				nextNetIface = netIfaces.nextElement();
				if(!nextNetIface.isLoopback() && nextNetIface.isUp()) {
					final Enumeration<InetAddress> addrs = nextNetIface.getInetAddresses();
					while(addrs.hasMoreElements()) {
						addr = addrs.nextElement();
						if(Inet4Address.class.isInstance(addr)) {
							LOG.debug(
								LogUtil.MSG, "Resolved external interface \"{}\" address: {}",
								nextNetIface.getDisplayName(), addr.getHostAddress()
							);
							break;
						}
					}
				} else {
					LOG.debug(
						LogUtil.MSG, "Interface \"{}\" is loopback or is not up, skipping",
						nextNetIface.getDisplayName()
					);
				}
			}
		} catch(final SocketException e) {
			LogUtil.failure(LOG, Level.WARN, e, "Failed to get an external interface address");
		}
		//
		if(addr == null) {
			LOG.warn(
				LogUtil.ERR, "No valid external interface have been found, falling back to loopback"
			);
			addr = InetAddress.getLoopbackAddress();
		}
		//
		return addr.getHostAddress();
	}
	//
	public static long getHostAddrCode() {
		return (long) getHostAddr().hashCode() << Integer.SIZE;
	}
	//
	public static Remote create(final Service svc) {
		RemoteStub stub = null;
		try {
			stub = UnicastRemoteObject.exportObject(svc);
			LOG.debug(LogUtil.MSG, "Exported service object successfully");
		} catch(final RemoteException e) {
			LogUtil.failure(LOG, Level.FATAL, e, "Failed to export service object");
		}
		//
		if(stub != null) {
			try {
				final String rmiHostName = System.getProperty(JAVA_RMI_SERVER_HOSTNAME);
				final String svcName = (rmiHostName != null) ?
					rmiHostName : svc.getName();
				Naming.rebind(svcName, svc);
				SVC_MAP.put(svcName, svc);
				LOG.info(LogUtil.MSG, "New service bound: {}", svcName);
			} catch(final RemoteException e) {
				LOG.error(LogUtil.ERR, "Failed to rebind the service", e);
			} catch(final MalformedURLException e) {
				LOG.error(LogUtil.ERR, "Invalid URL", e);
			}
		}
		//
		return svc;
	}
	/**
	 Get the service created earlier if exists
	 @param svcName
	 @return
	 */
	public static Service getLocalSvc(final String svcName) {
		return SVC_MAP.get(svcName);
	}
	/**
	 Connect to server service
	 @param url
	 @return
	 */
	public static Service getRemoteSvc(final String url) {
		Remote remote = null;
		Service remoteSvc = null;
		try {
			remote = Naming.lookup(url);
			remoteSvc = Service.class.cast(remote);
		} catch(final ClassCastException e) {
			if(remote==null) {
				LOG.error(LogUtil.ERR, "Lookup method returns null");
			} else {
				LOG.error(
					LogUtil.ERR, "Unsupported type of the resolved service: {}", remote.getClass()
				);
			}
		} catch(MalformedURLException e) {
			LOG.error(LogUtil.ERR, "Looks like bad URL: \"{}\"", url);
		} catch(final NotBoundException e) {
			LOG.error(LogUtil.ERR, "No service bound with url \"{}\"", url);
		} catch(final RemoteException e) {
			LogUtil.failure(LOG, Level.WARN, e, "Looks like network failure");
		}
		return remoteSvc;
	}
	//
	public static void close(final Service svc) {
		try {
            UnicastRemoteObject.unexportObject(svc, true);
			LOG.debug(LogUtil.MSG, "Unexported service object");
		} catch(NoSuchObjectException e) {
			LogUtil.failure(LOG, Level.WARN, e, "Failed to unexport service object");
		}
		//
		try {
			final String svcName = svc.getName();
			Naming.unbind(svcName);
			SVC_MAP.remove(svcName);
			LOG.info(LogUtil.MSG, "Removed service: {}", svcName);
		} catch(final NotBoundException e) {
			LOG.debug(LogUtil.ERR, "Service not bound");
		} catch(final RemoteException e) {
			LOG.warn(LogUtil.ERR, "Possible connection failure", e);
		} catch(final MalformedURLException e) {
			LOG.error(LogUtil.ERR, "Invalid URL", e);
		}
	}
	//
	public final static String
		KEY_RMI_CODEBASE = "java.rmi.server.codebase",
		KEY_JMX_AUTH = "com.sun.management.jmxremote.authenticate",
		KEY_JMX_PORT = "com.sun.management.jmxremote.port",
		KEY_JMX_SSL = "com.sun.management.jmxremote.ssl",
		JMXRMI_URL_PREFIX = "service:jmx:rmi:///jndi/rmi://",
		JMXRMI_URL_PATH = "/jmxrmi";
	//
	private final static Map<Integer, MBeanServer>
		MBEAN_SERVERS = new ConcurrentHashMap<>();
	private final static Collection<JMXConnectorServer>
		JMX_CONNECTOR_SERVERS = new ConcurrentLinkedQueue<>();
	public static MBeanServer getMBeanServer(final int portJmxRmi) {
		//
		MBeanServer mBeanServer;
		//
		if(MBEAN_SERVERS.containsKey(portJmxRmi)) {
			mBeanServer = MBEAN_SERVERS.get(portJmxRmi);
		} else {
			try {
				System.setProperty(
					KEY_RMI_CODEBASE,
					URLDecoder.decode(
						JAR_SELF.toURI().toString(), StandardCharsets.UTF_8.displayName()
					)
				);
			} catch(final UnsupportedEncodingException e) {
				LogUtil.failure(LOG, Level.WARN, e, "Setting system property failure");
			}
			LOG.debug(LogUtil.MSG, "RMI codebase: {}", System.getProperty(KEY_RMI_CODEBASE));
			//
			System.setProperty(KEY_JMX_PORT, Integer.toString(portJmxRmi));
			LOG.debug(LogUtil.MSG, "RMI JMX port: {}", System.getProperty(KEY_JMX_PORT));
			//
			mBeanServer = ManagementFactory.getPlatformMBeanServer();
			//
			try {
				LocateRegistry.createRegistry(portJmxRmi);
				LOG.debug(LogUtil.MSG, "Created locate registry for port {}", portJmxRmi);
			} catch(final RemoteException e) {
				LogUtil.failure(
					LOG, Level.WARN, e,
					String.format("Failed to create registry for port %d", portJmxRmi)
				);
			}
			//
			final Map<String, Object> env = new HashMap<>();
			env.put(KEY_JMX_AUTH, String.valueOf(false));
			env.put(KEY_JMX_SSL, String.valueOf(false));
			//
			JMXServiceURL jmxSvcURL = null;
			try {
				jmxSvcURL = new JMXServiceURL(
					JMXRMI_URL_PREFIX + ":" + Integer.toString(portJmxRmi) +
					JMXRMI_URL_PATH + Integer.toString(portJmxRmi)
				);
				LOG.debug(LogUtil.MSG, "Created JMX service URL {}", jmxSvcURL.toString());
			} catch(final MalformedURLException e) {
				LogUtil.failure(
					LOG, Level.WARN, e,
					String.format("Failed to create JMX service URL for port #%d", portJmxRmi)
				);
			}
			//
			JMXConnectorServer connectorServer = null;
			if(jmxSvcURL != null) {
				try {
					//LOG.trace(Markers.MSG, "{}, {}, {}", jmxSvcURL, env, mBeanServer);
					connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
						jmxSvcURL, env, mBeanServer
					);
					JMX_CONNECTOR_SERVERS.add(connectorServer); // remember for shutdown
					LOG.debug(LogUtil.MSG, "Created JMX connector");
				} catch(final IOException e) {
					LogUtil.failure(LOG, Level.WARN, e, "Failed to create JMX connector");
				}
			}
			//
			if(connectorServer != null && !connectorServer.isActive()) {
				try {
					connectorServer.start();
					LOG.debug(LogUtil.MSG, "JMX connector started", portJmxRmi);
				} catch(final IOException e) {
					LogUtil.failure(
						LOG, Level.WARN, e,
						"Failed to start JMX connector, please check that there's no another instance running"
					);
				}
			}
			//
			MBEAN_SERVERS.put(portJmxRmi, mBeanServer);
		}
		//
		return mBeanServer;
	}
	//
	public static void shutdown() {
		//
		for(final Service svc : SVC_MAP.values()) {
			close(svc);
		}
		/*
		try {
			for(final Service svc : SVC_MAP.values()) {
				close(svc);
			}
			//
			final Registry registry = LocateRegistry.getRegistry(PORT_RMI_CONTROL);
			final String names[] = registry.list();
			if(names.length > 0) {
				LOG.debug(LogUtil.MSG, "Not closed services: {}", Arrays.toString(names));
				for(final String name : names) {
					try{
						registry.unbind(name);
					} catch(final NotBoundException e) {
						LogUtil.failure(
							LOG, Level.DEBUG, e,
							String.format("Service \"%s\" is not bound", name)
						);
					}
				}
			}
			//
		} catch(final RemoteException e) {
			LogUtil.failure(
				LOG, Level.WARN, e,
				String.format("Failed to get a registry for port #%d", PORT_RMI_CONTROL)
			);
		}
		*/
		//
		for(final JMXConnectorServer jmxConnectorServer : JMX_CONNECTOR_SERVERS) {
			if(jmxConnectorServer.isActive()) {
				try {
					jmxConnectorServer.stop();
				} catch(final IOException e) {
					LogUtil.failure(
						LOG, Level.WARN, e,
						String.format(
							"Failed to stop JMX connector server @%s",
							jmxConnectorServer.getAddress()
						)
					);
				}
			}
		}
	}
	//
}
