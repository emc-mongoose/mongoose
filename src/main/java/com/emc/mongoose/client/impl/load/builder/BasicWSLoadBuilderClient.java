package com.emc.mongoose.client.impl.load.builder;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.data.WSObject;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
// mongoose-common.jar
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.load.model.FileProducer;
import com.emc.mongoose.core.impl.io.req.conf.WSRequestConfigBase;
// mongoose-client.jar
import com.emc.mongoose.client.impl.load.executor.BasicWSLoadClient;
import com.emc.mongoose.client.api.load.builder.WSLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.WSLoadClient;
//
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 Created by kurila on 08.05.14.
 */
public final class BasicWSLoadBuilderClient<T extends WSObject, U extends WSLoadClient<T>>
extends LoadBuilderClientBase<T, U>
implements WSLoadBuilderClient<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSLoadBuilderClient()
	throws IOException {
		super();
	}
	//
	public BasicWSLoadBuilderClient(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig);
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
		WSLoadBuilderSvc<T, U> rlb;
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
	public final BasicWSLoadBuilderClient<T, U> setInputFile(final String listFile)
	throws RemoteException {
		if(listFile!=null) {
			try {
				srcProducer = (FileProducer<T>) new FileProducer<>(getMaxCount(), listFile, BasicWSObject.class);
				LOG.info(LogUtil.MSG, "Local data items will be read from file @ \"{}\"", listFile);
			} catch(final NoSuchMethodException | IOException e) {
				LOG.error(LogUtil.ERR, "Failure", e);
			}
		}
		return this;
	}
	//
	@Override
	protected final void invokePreConditions()
	throws IllegalStateException {
		reqConf.configureStorage(dataNodeAddrs);
	}
	//
	@Override  @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws RemoteException {
		//
		WSLoadClient newLoadClient;
		//
		final Map<String, LoadSvc<T>> remoteLoadMap = new ConcurrentHashMap<>();
		final Map<String, JMXConnector> remoteJMXConnMap = new ConcurrentHashMap<>();
		//
		LoadBuilderSvc<T, U> nextBuilder;
		LoadSvc<T> nextLoad;
		//
		String svcJMXAddr;
		JMXServiceURL nextJMXURL;
		JMXConnector nextJMXConn;
		final int jmxImportPort = runTimeConfig.getRemotePortImport();
		//
		for(final String addr : keySet()) {
			//
			nextBuilder = get(addr);
			nextBuilder.setRequestConfig(reqConf); // should upload req conf right before instancing
			nextLoad = (LoadSvc<T>) ServiceUtils.getRemoteSvc(
				String.format("//%s/%s", addr, nextBuilder.buildRemotely())
			);
			remoteLoadMap.put(addr, nextLoad);
			//
			nextJMXURL = null;
			try {
				svcJMXAddr = ServiceUtils.JMXRMI_URL_PREFIX + addr + ":" +
					Integer.toString(jmxImportPort) + ServiceUtils.JMXRMI_URL_PATH +
					Integer.toString(jmxImportPort);
				nextJMXURL = new JMXServiceURL(svcJMXAddr);
				LOG.debug(LogUtil.MSG, "Server JMX URL: {}", svcJMXAddr);
			} catch(final MalformedURLException e) {
				LogUtil.failure(LOG, Level.ERROR, e, "Failed to generate JMX URL");
			}
			//
			nextJMXConn = null;
			if(nextJMXURL != null) {
				try {
					nextJMXConn = JMXConnectorFactory.connect(nextJMXURL, null);
				} catch(final IOException e) {
					LogUtil.failure(
						LOG, Level.ERROR, e,
						String.format("Failed to connect to \"%s\" via JMX", nextJMXURL)
					);
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
			runTimeConfig.getLoadLimitCount(), srcProducer
		);
		if(srcProducer != null && srcProducer.getConsumer() == null) {
			LOG.debug(
				LogUtil.MSG, "Append consumer {} for producer {}",
				newLoadClient.getName(), srcProducer.getName()
			);
			srcProducer.setConsumer(newLoadClient);
		}
		srcProducer = null;
		LOG.debug(LogUtil.MSG, "Load client {} created", newLoadClient.getName());
		//
		return (U) newLoadClient;
	}
}
