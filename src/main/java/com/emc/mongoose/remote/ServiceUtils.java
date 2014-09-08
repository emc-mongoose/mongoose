package com.emc.mongoose.remote;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.run.Main;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
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
import java.util.HashMap;
/**
 Created by kurila on 05.05.14.
 */
public final class ServiceUtils {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static HashMap<String, Service> SVC_MAP;
	//private final static Registry REGISTRY;
	//
	static {
		// set up security manager
		if(System.getSecurityManager()==null) {
			final SecurityManager sm = new SecurityManager();
			LOG.trace(Markers.MSG, "New security manager instance created");
			System.setSecurityManager(sm);
		}
		//
		SVC_MAP = new HashMap<>();
		//
		// create or use existing registry
		//Registry registry = null;
		try {
			/*registry = */LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			LOG.debug(Markers.MSG, "RMI registry created");
		} catch(final RemoteException e) {
			try {
				/*registry = */LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
				LOG.info(Markers.MSG, "Reusing already existing RMI registry");
			} catch(final RemoteException ee) {
				LOG.fatal(Markers.ERR, "Failed to obtain RMI registry", ee);
			}
		}
		//REGISTRY = registry;
	}
	//
	public static String getHostAddr() {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
		} catch(final UnknownHostException e) {
			LOG.warn(Markers.ERR, "Failed to get host address: {}", e.toString());
			if(LOG.isTraceEnabled()) {
				final Throwable cause = e.getCause();
				if(cause!=null) {
					LOG.trace(Markers.ERR, cause.toString(), cause.getCause());
				}
			}
			addr = InetAddress.getLoopbackAddress();
		}
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
			LOG.debug(Markers.MSG, "Exported service object");
		} catch(final RemoteException e) {
			LOG.error(Markers.ERR, "Failed to export service object", e);
		}
		//
		if(stub!=null) {
			try {
				final String svcName = svc.getName();
				Naming.rebind(svcName, svc);
				SVC_MAP.put(svcName, svc);
				LOG.info(Markers.MSG, "New service bound: {}", svcName);
			} catch(final RemoteException e) {
				LOG.error(Markers.ERR, "Failed to rebind the service", e);
			} catch(final MalformedURLException e) {
				LOG.error(Markers.ERR, "Invalid URL", e);
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
	 Connect to driver service
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
				LOG.error(Markers.ERR, "Lookup method returns null");
			} else {
				LOG.error(
					Markers.ERR, "Unsupported type of the resolved service: {}", remote.getClass()
				);
			}
		} catch(MalformedURLException e) {
			LOG.error(Markers.ERR, "Looks like bad URL: \"{}\"", url);
		} catch(final NotBoundException e) {
			LOG.error(Markers.ERR, "No service bound with url \"{}\"", url);
		} catch(final RemoteException e) {
			LOG.error(Markers.ERR, "Looks like network failure", e.toString());
			if(LOG.isTraceEnabled()) {
				final Throwable cause = e.getCause();
				if(cause!=null) {
					LOG.trace(Markers.ERR, cause.toString(), cause.getCause());
				}
			}
		}
		return remoteSvc;
	}
	//
	public static void close(final Service svc) {
		try {
			UnicastRemoteObject.unexportObject(svc, true);
			LOG.debug(Markers.MSG, "Unexported service object");
		} catch(NoSuchObjectException e) {
			LOG.warn(Markers.ERR, "No such service object", e);
		}
		//
		try {
			final String svcName = svc.getName();
			Naming.unbind(svcName);
			SVC_MAP.remove(svcName);
			LOG.info(Markers.MSG, "Removed service: {}", svcName);
		} catch(final NotBoundException e) {
			LOG.warn(Markers.ERR, "Service not bound");
		} catch(final RemoteException e) {
			LOG.error(Markers.ERR, "Possible connection failure", e);
		} catch(final MalformedURLException e) {
			LOG.error(Markers.ERR, "Invalid URL", e);
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
	public static MBeanServer getMBeanServer(final int portJmxRmi) {
		//
		try {
			System.setProperty(
				KEY_RMI_CODEBASE,
				URLDecoder.decode(
					Main.JAR_SELF.toURI().toString(), StandardCharsets.UTF_8.displayName()
				)
			);
		} catch(final UnsupportedEncodingException e) {
			LOG.warn(Markers.ERR, e.toString(), e.getCause());
		}
		LOG.debug(Markers.MSG, "RMI codebase: {}", System.getProperty(KEY_RMI_CODEBASE));
		//
		System.setProperty(KEY_JMX_PORT, Integer.toString(portJmxRmi));
		LOG.debug(Markers.MSG, "RMI JMX port: {}", System.getProperty(KEY_JMX_PORT));
		//
		final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		//
		try {
			LocateRegistry.createRegistry(portJmxRmi);
			LOG.debug(Markers.MSG, "Created locate registry for port {}", portJmxRmi);
		} catch(final RemoteException e) {
			synchronized(LOG) {
				LOG.debug(
					Markers.ERR, "Failed to create registry for port {}: ", portJmxRmi, e.toString()
				);
			}
		}
		//
		final HashMap<String,Object> env = new HashMap<>();
		env.put(KEY_JMX_AUTH, String.valueOf(false));
		env.put(KEY_JMX_SSL, String.valueOf(false));
		//
		JMXServiceURL jmxSvcURL = null;
		try {
			jmxSvcURL = new JMXServiceURL(
				JMXRMI_URL_PREFIX + ":" + Integer.toString(portJmxRmi) + JMXRMI_URL_PATH
			);
			LOG.debug(Markers.MSG, "Created JMX service URL {}", jmxSvcURL.toString());
		} catch(final MalformedURLException e) {
			synchronized(LOG) {
				LOG.warn(Markers.ERR, "Failed to create JMX service URL for port {}", portJmxRmi);
				LOG.debug(Markers.ERR, e.toString(), e.getCause());
			}
		}
		//
		JMXConnectorServer connectorServer = null;
		if(jmxSvcURL!=null) {
			try {
				connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
					jmxSvcURL, env, mBeanServer
				);
				LOG.debug(Markers.MSG, "Created JMX connector");
			} catch(final IOException e) {
				synchronized(LOG) {
					LOG.warn(Markers.ERR, "Failed to create JMX connector for env {}", env);
					LOG.debug(Markers.ERR, e.toString(), e.getCause());
				}
			}
		}
		//
		if(connectorServer!=null) {
			try {
				connectorServer.start();
				LOG.debug(Markers.MSG, "JMX connector started", portJmxRmi);
			} catch(final IOException e) {
				synchronized(LOG) {
					LOG.warn(Markers.ERR, "Failed to start JMX connector for env {}", env);
					LOG.debug(Markers.ERR, e.toString(), e.getCause());
				}
			}
		}
		//
		return mBeanServer;
	}
	//
	
}
