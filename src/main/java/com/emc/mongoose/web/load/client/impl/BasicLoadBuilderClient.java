package com.emc.mongoose.web.load.client.impl;
//
import com.emc.mongoose.base.load.client.impl.LoadBuilderClientBase;
import com.emc.mongoose.base.load.server.LoadBuilderSvc;
import com.emc.mongoose.util.logging.MessageFactoryImpl;
import com.emc.mongoose.util.remote.Service;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.data.impl.BasicWSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.base.data.persist.FileProducer;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.util.remote.ServiceUtils;
//
import com.emc.mongoose.web.load.client.WSLoadBuilderClient;
import com.emc.mongoose.web.load.client.WSLoadClient;
import com.emc.mongoose.web.load.server.WSLoadBuilderSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
/**
 Created by kurila on 08.05.14.
 */
public final class BasicLoadBuilderClient<T extends WSObject, U extends WSLoadClient<T>>
extends LoadBuilderClientBase<T, U>
implements WSLoadBuilderClient<T, U> {
	//
	protected Logger log;
	//
	public BasicLoadBuilderClient()
	throws IOException {
		super();
	}
	//
	public BasicLoadBuilderClient(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig);
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSRequestConfig<T> getDefaultRequestConfig() {
		return (WSRequestConfig<T>) WSRequestConfigBase.getInstance();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSLoadBuilderSvc<T, U> resolve(final String serverAddr)
	throws IOException {
		WSLoadBuilderSvc<T, U> rlb = null;
		final Service remoteSvc = ServiceUtils.getRemoteSvc(
			"//" + serverAddr + '/' + getClass().getPackage().getName().replace("client", "server")
		);
		if(remoteSvc==null) {
			throw new IOException("No remote load builder was resolved from " + serverAddr);
		} else if(WSLoadBuilderSvc.class.isInstance(remoteSvc)) {
			rlb = WSLoadBuilderSvc.class.cast(remoteSvc);
		} else {
			throw new IOException(
				"Illegal class "+remoteSvc.getClass().getCanonicalName()+
					" of the instance resolved from "+serverAddr
			);
		}
		return rlb;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final BasicLoadBuilderClient<T, U> setInputFile(final String listFile)
	throws RemoteException {
		if(listFile!=null) {
			try {
				srcProducer = (FileProducer<T>) new FileProducer<>(listFile, BasicWSObject.class);
				log.info(Markers.MSG, "Local data items will be read from file @ \"{}\"", listFile);
			} catch(final NoSuchMethodException | IOException e) {
				log.error(Markers.ERR, "Failure", e);
			}
		}
		return this;
	}
	//
	@Override  @SuppressWarnings("unchecked")
	public final U build()
	throws RemoteException {
		//
		com.emc.mongoose.web.load.client.WSLoadClient newLoadClient = null;
		//
		final Map<String, LoadSvc<T>> remoteLoadMap = new HashMap<>();
		final Map<String, JMXConnector> remoteJMXConnMap = new HashMap<>();
		//
		LoadBuilderSvc<T, U> nextBuilder = null;
		LoadSvc<T> nextLoad = null;
		//
		String svcJMXAddr;
		JMXServiceURL nextJMXURL;
		JMXConnector nextJMXConn;
		//
		try {
			reqConf.configureStorage(); // should be done after configuring and before req conf upload
		} catch(final IllegalStateException e) {
			ExceptionHandler.trace(log, Level.ERROR, e, "Failed to configure storage");
		}
		//
		for(final String addr : keySet()) {
			//
			nextBuilder = get(addr);
			nextBuilder.setRequestConfig(reqConf); // should upload req conf right before instancing
			nextLoad = (LoadSvc<T>) ServiceUtils.getRemoteSvc(
				"//" + addr + "/" + nextBuilder.buildRemotely()
			);
			remoteLoadMap.put(addr, nextLoad);
			//
			nextJMXURL = null;
			try {
				svcJMXAddr = ServiceUtils.JMXRMI_URL_PREFIX + addr + ":" +
					runTimeConfig.getRemoteMonitorPort() + ServiceUtils.JMXRMI_URL_PATH;
				nextJMXURL = new JMXServiceURL(svcJMXAddr);
				log.debug(Markers.MSG, "Server JMX URL: {}", svcJMXAddr);
			} catch(final MalformedURLException e) {
				log.error(Markers.ERR, "Failure", e);
			}
			//
			nextJMXConn = null;
			if(nextJMXURL!=null) {
				try {
					nextJMXConn = JMXConnectorFactory.connect(nextJMXURL, null);
				} catch(final IOException e) {
					log.error(Markers.ERR, "JMX: failed to connect to " + nextJMXURL, e);
				}
			}
			//
			if(nextJMXConn!=null) {
				remoteJMXConnMap.put(addr, nextJMXConn);
			}
			//
		}
		//
		newLoadClient = new BasicWSLoadClient<>(
			runTimeConfig, remoteLoadMap, remoteJMXConnMap, (WSRequestConfig<T>) reqConf,
			runTimeConfig.getDataCount(), nextLoad==null ? 1 : nextLoad.getThreadCount()
		);
		log.debug(Markers.MSG, "Load client {} created", newLoadClient.getName());
		if(srcProducer!=null && srcProducer.getConsumer()==null) {
			log.debug(
					Markers.MSG, "Append consumer {} for producer {}",
					newLoadClient.getName(), srcProducer.getName()
			);
			srcProducer.setConsumer(newLoadClient);
			srcProducer.start();
			srcProducer = null;
		}
		//
		return (U) newLoadClient;
	}
}
