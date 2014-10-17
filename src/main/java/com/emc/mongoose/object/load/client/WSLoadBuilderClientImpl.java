package com.emc.mongoose.object.load.client;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.load.server.LoadBuilderSvc;
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.api.WSRequestConfigBase;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.object.load.server.WSLoadBuilderSvc;
import com.emc.mongoose.object.load.server.WSLoadBuilderSvcImpl;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.base.data.persist.FileProducer;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.base.load.server.LoadSvc;
import com.emc.mongoose.util.remote.Service;
import com.emc.mongoose.util.remote.ServiceUtils;
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
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
/**
 Created by kurila on 08.05.14.
 */
public final class WSLoadBuilderClientImpl<T extends WSObjectImpl, U extends WSLoadClient<T>>
extends HashMap<String, WSLoadBuilderSvc<T, U>>
implements WSLoadBuilderClient<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private FileProducer<T> srcProducer = null;
	private volatile RunTimeConfig runTimeConfig;
	@SuppressWarnings("unchecked")
	private volatile WSRequestConfig<T> reqConf = WSRequestConfigBase.getInstance();
	//
	@SuppressWarnings("unchecked")
	private WSLoadBuilderSvc<T, U> resolve(final String serverAddr)
	throws IOException {
		WSLoadBuilderSvc<T, U> rlb = null;
		final Service remoteSvc = ServiceUtils.getRemoteSvc(
			"//" + serverAddr + '/' + WSLoadBuilderSvcImpl.class.getSimpleName()
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
	public WSLoadBuilderClientImpl()
	throws IOException {
		this(Main.RUN_TIME_CONFIG);
	}
	//
	public WSLoadBuilderClientImpl(final RunTimeConfig runTimeConfig)
	throws IOException {
		super(runTimeConfig.getRemoteServers().length);
		this.runTimeConfig = runTimeConfig;
		final String remoteServers[] = runTimeConfig.getRemoteServers();
		for(final String serverAddr: remoteServers) {
			LOG.info(Markers.MSG, "Resolving server service @ \"{}\"...", serverAddr);
			put(serverAddr, resolve(serverAddr));
		}
	}
	//
	@Override @SuppressWarnings("AccessStaticViaInstance")
	public final WSLoadBuilderClientImpl<T, U> setProperties(final RunTimeConfig runTimeConfig)
	throws RemoteException {
		//
		this.runTimeConfig = runTimeConfig;
		reqConf.setProperties(runTimeConfig);
		//
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			LOG.info(Markers.MSG, "Applying the properties to server @ \"{}\"...", addr);
			nextBuilder.setProperties(runTimeConfig);
		}
		//
		final String firstNodeAddr = reqConf.getAddr();
		if(firstNodeAddr == null || firstNodeAddr.length() == 0) {
			final String nodeAddrs[] = runTimeConfig.getStorageAddrs();
			if(nodeAddrs != null && nodeAddrs.length > 0) {
				reqConf.setAddr(nodeAddrs[0]);
			}
		}
		//
		String dataMetaInfoFile = null;
		try {
			dataMetaInfoFile = this.runTimeConfig.getDataSrcFPath();
			if(
				dataMetaInfoFile!=null && dataMetaInfoFile.length()>0 &&
				Files.isReadable(Paths.get(dataMetaInfoFile))
			) {
				setInputFile(dataMetaInfoFile);
			}
		} catch(final NoSuchElementException e) {
			LOG.warn(Markers.ERR, "No \"data.src.fpath\" property available");
		} catch(final InvalidPathException e) {
			LOG.warn(Markers.ERR, "Invalid data metainfo src file path: {}", dataMetaInfoFile);
		} catch(final SecurityException e) {
			LOG.warn(Markers.ERR, "Unexpected exception", e);
		}
		return this;
	}
	//
	@Override
	public final WSRequestConfig<T> getRequestConfig() {
		return reqConf;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws ClassCastException, RemoteException {
		this.reqConf = (WSRequestConfig<T>) reqConf;
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException {
		reqConf.setLoadType(loadType);
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setLoadType(loadType);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMaxCount(maxCount);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMinObjSize(minObjSize);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMaxObjSize(maxObjSize);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadsPerNodeDefault(threadCount);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setThreadsPerNodeFor(
		final short threadCount, final Request.Type loadType
	) throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadsPerNodeFor(threadCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		// need to remember 1st storage node address to configure later
		final String firstNodeAddr = reqConf.getAddr();
		if(firstNodeAddr == null || firstNodeAddr.length() == 0) {
			if(dataNodeAddrs != null && dataNodeAddrs.length > 0) {
				reqConf.setAddr(dataNodeAddrs[0]);
			}
		}
		//
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setDataNodeAddrs(dataNodeAddrs);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClientImpl<T, U> setUpdatesPerItem(int count)
	throws RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setUpdatesPerItem(count);
		}
		return null;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final WSLoadBuilderClientImpl<T, U> setInputFile(final String listFile)
	throws RemoteException {
		if(listFile!=null) {
			try {
				srcProducer = (FileProducer<T>) new FileProducer<>(listFile, WSObjectImpl.class);
				LOG.info(Markers.MSG, "Local data items will be read from file @ \"{}\"", listFile);
			} catch(final NoSuchMethodException | IOException e) {
				LOG.error(Markers.ERR, "Failure", e);
			}
		}
		return this;
	}
	//
	//
	@Override
	public final long getMaxCount()
	throws RemoteException {
		return values().iterator().next().getMaxCount();
	}
	//
	@Override  @SuppressWarnings("unchecked")
	public final U build()
	throws RemoteException {
		//
		WSLoadClient<T> newLoadClient = null;
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
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to configure storage");
		}
		//
		for(final String addr : keySet()) {
			//
			nextBuilder = get(addr);
			/*
			LOG.info(Markers.MSG, "Test req conf serialization begin {}", addr);
			try(final FileOutputStream fileOut = new FileOutputStream("/tmp/req.conf.bin")) {
				reqConf.writeExternal(new ObjectOutputStream(fileOut));
				LOG.info("Serialization success");
			} catch(final IOException e) {
				ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to serialize req conf");
			}
			try(final FileInputStream fileIn = new FileInputStream("/tmp/req.conf.bin")) {
				reqConf.readExternal(new ObjectInputStream(fileIn));
				LOG.info("Deserialization success");
			} catch(final ClassNotFoundException | IOException e) {
				ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to deserialize req conf");
			}
			LOG.info(Markers.MSG, "Test req conf serialization done {}", addr);
			*/
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
				LOG.debug(Markers.MSG, "Server JMX URL: {}", svcJMXAddr);
			} catch(final MalformedURLException e) {
				LOG.error(Markers.ERR, "Failure", e);
			}
			//
			nextJMXConn = null;
			if(nextJMXURL!=null) {
				try {
					nextJMXConn = JMXConnectorFactory.connect(nextJMXURL, null);
				} catch(final IOException e) {
					LOG.error(Markers.ERR, "JMX: failed to connect to " + nextJMXURL, e);
				}
			}
			//
			if(nextJMXConn!=null) {
				remoteJMXConnMap.put(addr, nextJMXConn);
			}
			//
		}
		//
		newLoadClient = new WSLoadClientImpl<>(
			runTimeConfig, remoteLoadMap, remoteJMXConnMap, reqConf,
			runTimeConfig.getDataCount(), nextLoad==null ? 1 : nextLoad.getThreadCount()
		);
		LOG.debug(Markers.MSG, "Load client {} created", newLoadClient.getName());
		if(srcProducer!=null && srcProducer.getConsumer()==null) {
			LOG.debug(
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
