package com.emc.mongoose.object.http.controller;
//
import com.emc.mongoose.LoadBuilder;
import com.emc.mongoose.api.Request;
import com.emc.mongoose.api.RequestConfig;
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.UniformDataSource;
import com.emc.mongoose.data.persist.FileProducer;
import com.emc.mongoose.logging.Markers;
//
import com.emc.mongoose.object.http.data.WSObject;
import com.emc.mongoose.object.http.api.WSRequestConfig;
import com.emc.mongoose.object.http.driver.WSLoadBuilderService;
import com.emc.mongoose.remote.LoadBuilderService;
import com.emc.mongoose.remote.LoadService;
import com.emc.mongoose.remote.Service;
import com.emc.mongoose.remote.ServiceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
public final class WSLoadBuilderClient
extends HashMap<String, LoadBuilderService<LoadService<WSObject>>>
implements LoadBuilder {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private FileProducer<WSObject> srcProducer = null;
	//
	@SuppressWarnings("unchecked")
	private LoadBuilderService<LoadService<WSObject>> resolve(final String driverAddr)
	throws IOException {
		LoadBuilderService<LoadService<WSObject>> rlb = null;
		final Service remoteSvc = ServiceUtils.getRemoteSvc(
			"//" + driverAddr + '/' + WSLoadBuilderService.class.getSimpleName()
		);
		if(remoteSvc==null) {
			throw new IOException("No remote load builder was resolved from "+driverAddr);
		} else if(LoadBuilderService.class.isInstance(remoteSvc)) {
			rlb = LoadBuilderService.class.cast(remoteSvc);
		} else {
			throw new IOException(
				"Illegal class "+remoteSvc.getClass().getCanonicalName()+
				" of the instance resolved from "+driverAddr
			);
		}
		return rlb;
	}
	//
	public WSLoadBuilderClient()
	throws IOException {
		this(RunTimeConfig.getStringArray("remote.drivers"));
	}
	//
	private WSLoadBuilderClient(final String driverAddrs[])
	throws IOException {
		super(driverAddrs.length);
		for(final String driverAddr: driverAddrs) {
			LOG.info(Markers.MSG, "Resolving driver service @ \"{}\"...", driverAddr);
			put(driverAddr, resolve(driverAddr));
		}
	}
	//
	@Override
	public final WSLoadBuilderClient setProperties(final RunTimeConfig props)
	throws RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			LOG.info(Markers.MSG, "Applying the properties to driver service @ \"{}\"...", addr);
			nextBuilder.setProperties(props);
		}
		//
		String dataMetaInfoFile = null;
		try {
			dataMetaInfoFile = RunTimeConfig.getString("data.src.fpath");
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
	public final WSLoadBuilderClient setRequestConfig(final RequestConfig reqConf)
	throws ClassCastException, RemoteException {
		final WSRequestConfig wsReqConf = WSRequestConfig.class.cast(reqConf);
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRequestConfig(wsReqConf);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setLoadType(loadType);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMaxCount(maxCount);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMinObjSize(minObjSize);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMaxObjSize(maxObjSize);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadsPerNodeDefault(threadCount);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient setThreadsPerNodeFor(
		final short threadCount, final Request.Type loadType
	) throws IllegalArgumentException, RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadsPerNodeFor(threadCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setDataNodeAddrs(dataNodeAddrs);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient setInputFile(final String listFile)
	throws RemoteException {
		if(listFile!=null) {
			try {
				srcProducer = new FileProducer<>(listFile, WSObject.class);
				LOG.info(Markers.MSG, "Local data items will be read from file @ \"{}\"", listFile);
			} catch(final NoSuchMethodException | IOException e) {
				LOG.error(Markers.ERR, "Failure", e);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilder setUpdatesPerItem(int count)
	throws RemoteException {
		LoadBuilderService<LoadService<WSObject>> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setUpdatesPerItem(count);
		}
		return null;
	}
	//
	@Override
	public final long getMaxCount()
	throws RemoteException {
		return values().iterator().next().getMaxCount();
	}
	//
	@Override  @SuppressWarnings("unchecked")
	public final WSLoadClient build()
	throws URISyntaxException, RemoteException {
		//
		WSLoadClient newLoadClient;
		//
		final Map<String, LoadService<WSObject>> remoteLoadMap = new HashMap<>();
		final Map<String, JMXConnector> remoteJMXConnMap = new HashMap<>();
		//
		LoadBuilderService<LoadService<WSObject>> nextBuilder = null;
		LoadService<WSObject> nextLoad = null;
		//
		String svcJMXAddr;
		JMXServiceURL nextJMXURL;
		JMXConnector nextJMXConn;
		//
		for(final String addr: keySet()) {
			//
			nextBuilder = get(addr);
			nextLoad = LoadService.class.cast(
				ServiceUtils.getRemoteSvc(
					"//"+addr+"/"+nextBuilder.buildRemotely()
				)
			);
			remoteLoadMap.put(addr, nextLoad);
			//
			nextJMXURL = null;
			try {
				svcJMXAddr = ServiceUtils.JMXRMI_URL_PREFIX + addr + ":" +
					RunTimeConfig.getString("remote.monitor.port") + ServiceUtils.JMXRMI_URL_PATH;
				nextJMXURL = new JMXServiceURL(svcJMXAddr);
				LOG.debug(Markers.MSG, "Driver JMX URL: {}", svcJMXAddr);
			} catch(final MalformedURLException e) {
				LOG.error(Markers.ERR, "Failure", e);
			}
			//
			nextJMXConn = null;
			if(nextJMXURL!=null) {
				try {
					nextJMXConn = JMXConnectorFactory.connect(nextJMXURL, null);
				} catch(final IOException e) {
					LOG.error(Markers.ERR, "JMX: failed to connect to "+nextJMXURL, e);
				}
			}
			//
			if(nextJMXConn!=null) {
				remoteJMXConnMap.put(addr, nextJMXConn);
			}
			//
		}
		//
		newLoadClient = new WSLoadClient(
			remoteLoadMap, remoteJMXConnMap, RunTimeConfig.getLong("data.count"),
			nextLoad==null? 1 : nextLoad.getThreadCount()
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
		return newLoadClient;
	}
}
