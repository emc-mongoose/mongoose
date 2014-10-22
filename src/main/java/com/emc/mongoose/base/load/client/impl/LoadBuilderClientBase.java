package com.emc.mongoose.base.load.client.impl;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.persist.FileProducer;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.client.LoadBuilderClient;
import com.emc.mongoose.base.load.server.LoadBuilderSvc;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.NoSuchElementException;
/**
 Created by kurila on 20.10.14.
 */
public abstract class LoadBuilderClientBase<T extends DataItem, U extends LoadExecutor<T>>
extends HashMap<String, LoadBuilderSvc<T, U>>
implements LoadBuilderClient<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected FileProducer<T> srcProducer = null;
	protected volatile RunTimeConfig runTimeConfig;
	protected volatile RequestConfig<T> reqConf;
	//
	{
		reqConf = getDefaultRequestConfig();
	}
	//
	public LoadBuilderClientBase()
	throws IOException {
		this(Main.RUN_TIME_CONFIG);
	}
	//
	public LoadBuilderClientBase(final RunTimeConfig runTimeConfig)
	throws IOException {
		//
		super(runTimeConfig.getRemoteServers().length);
		this.runTimeConfig = runTimeConfig;
		final String remoteServers[] = runTimeConfig.getRemoteServers();
		//
		LoadBuilderSvc<T, U> loadBuilderSvc;
		int maxLastInstanceN = 0, nextInstanceN;
		for(final String serverAddr : remoteServers) {
			LOG.info(Markers.MSG, "Resolving server service @ \"{}\"...", serverAddr);
			loadBuilderSvc = resolve(serverAddr);
			nextInstanceN = loadBuilderSvc.getLastInstanceNum();
			if(nextInstanceN > maxLastInstanceN) {
				maxLastInstanceN = nextInstanceN;
			}
			put(serverAddr, loadBuilderSvc);
		}
		//
		for(final String serverAddr : remoteServers) {
			get(serverAddr).setLastInstanceNum(maxLastInstanceN);
		}
	}
	//
	protected abstract RequestConfig<T> getDefaultRequestConfig();
	//
	protected abstract LoadBuilderSvc<T, U> resolve(final String serverAddr)
	throws IOException;
	//
	@Override
	public final LoadBuilderClient<T, U> setProperties(final RunTimeConfig runTimeConfig)
	throws IllegalStateException, RemoteException {
		//
		this.runTimeConfig = runTimeConfig;
		if(reqConf != null) {
			reqConf.setProperties(runTimeConfig);
		} else {
			throw new IllegalStateException("Shared request config is not initialized");
		}
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
	public final RequestConfig<T> getRequestConfig() {
		return reqConf;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws ClassCastException, RemoteException {
		this.reqConf = reqConf;
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setLoadType(final Request.Type loadType)
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
	public final LoadBuilderClient<T, U> setMaxCount(final long maxCount)
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
	public final LoadBuilderClient<T, U> setMinObjSize(final long minObjSize)
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
	public final LoadBuilderClient<T, U> setMaxObjSize(final long maxObjSize)
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
	public final LoadBuilderClient<T, U> setThreadsPerNodeDefault(final short threadCount)
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
	public final LoadBuilderClient<T, U> setThreadsPerNodeFor(
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
	public final LoadBuilderClient<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
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
	public final LoadBuilderClient<T, U> setUpdatesPerItem(int count)
	throws RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
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
	@Override
	public abstract LoadBuilderClient<T, U> setInputFile(final String listFile)
	throws RemoteException;
	//
	@Override
	public abstract U build()
	throws RemoteException;
}
