package com.emc.mongoose.object.load.controller.impl;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.object.api.WSObjectRequestConfig;
import com.emc.mongoose.object.load.ObjectLoadExecutor;
import com.emc.mongoose.object.load.controller.ObjectLoadBuilderClient;
import com.emc.mongoose.object.load.driver.ObjectLoadBuilderService;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.base.data.persist.FileProducer;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.data.WSDataObject;
import com.emc.mongoose.object.load.driver.impl.WSLoadBuilderService;
import com.emc.mongoose.base.load.driver.LoadBuilderService;
import com.emc.mongoose.base.load.driver.LoadService;
import com.emc.mongoose.util.remote.Service;
import com.emc.mongoose.util.remote.ServiceUtils;
//
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
public final class WSLoadBuilderClient<T extends WSDataObject, U extends ObjectLoadExecutor<T>>
extends HashMap<String, LoadBuilderService<T, U>>
implements ObjectLoadBuilderClient<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private FileProducer<T> srcProducer = null;
	//
	@SuppressWarnings("unchecked")
	private LoadBuilderService<T, U> resolve(final String driverAddr)
	throws IOException {
		LoadBuilderService<T, U> rlb = null;
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
	public final WSLoadBuilderClient<T, U> setProperties(final RunTimeConfig props)
	throws RemoteException {
		LoadBuilderService<T, U> nextBuilder;
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
	public final WSLoadBuilderClient<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws ClassCastException, RemoteException {
		final WSObjectRequestConfig<T> wsReqConf = (WSObjectRequestConfig<T>) reqConf;
		LoadBuilderService<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRequestConfig(wsReqConf);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setLoadType(final Request.Type loadType)
	throws IllegalStateException, RemoteException {
		LoadBuilderService<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setLoadType(loadType);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMaxCount(maxCount);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMinObjSize(minObjSize);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMaxObjSize(maxObjSize);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadsPerNodeDefault(threadCount);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setThreadsPerNodeFor(
		final short threadCount, final Request.Type loadType
	) throws IllegalArgumentException, RemoteException {
		LoadBuilderService<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadsPerNodeFor(threadCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderService<T, U> nextBuilder;
		for(final String addr: keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setDataNodeAddrs(dataNodeAddrs);
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setInputFile(final String listFile)
	throws RemoteException {
		if(listFile!=null) {
			try {
				srcProducer = new FileProducer<>(listFile);
				LOG.info(Markers.MSG, "Local data items will be read from file @ \"{}\"", listFile);
			} catch(final NoSuchMethodException | IOException e) {
				LOG.error(Markers.ERR, "Failure", e);
			}
		}
		return this;
	}
	//
	@Override
	public final WSLoadBuilderClient<T, U> setUpdatesPerItem(int count)
	throws RemoteException {
		LoadBuilderService<T, U> nextBuilder;
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
	public final U build()
	throws URISyntaxException, RemoteException {
		//
		WSLoadClient<T> newLoadClient;
		//
		final Map<String, LoadService<T>> remoteLoadMap = new HashMap<>();
		final Map<String, JMXConnector> remoteJMXConnMap = new HashMap<>();
		//
		LoadBuilderService<T, U> nextBuilder = null;
		LoadService<T> nextLoad = null;
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
		newLoadClient = new WSLoadClient<>(
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
		return (U) newLoadClient;
	}
}
