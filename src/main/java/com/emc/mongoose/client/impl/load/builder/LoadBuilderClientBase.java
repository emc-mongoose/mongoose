package com.emc.mongoose.client.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.FileProducer;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.builder.LoadBuilderClient;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
//
import org.apache.logging.log4j.Level;
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
	protected String[] dataNodeAddrs = null;
	protected volatile RunTimeConfig runTimeConfig;
	protected volatile RequestConfig<T> reqConf;
	//
	{
		reqConf = getDefaultRequestConfig();
	}
	//
	public LoadBuilderClientBase()
	throws IOException {
		this(RunTimeConfig.getContext());
	}
	//
	public LoadBuilderClientBase(final RunTimeConfig runTimeConfig)
	throws IOException {
		//
		super(runTimeConfig.getLoadServers().length);
		final String remoteServers[] = runTimeConfig.getLoadServers();
		//
		LoadBuilderSvc<T, U> loadBuilderSvc;
		int maxLastInstanceN = 0, nextInstanceN;
		for(final String serverAddr : remoteServers) {
			LOG.info(Markers.MSG, "Resolving service @ \"{}\"...", serverAddr);
			loadBuilderSvc = resolve(serverAddr);
			nextInstanceN = loadBuilderSvc.getLastInstanceNum();
			if(nextInstanceN > maxLastInstanceN) {
				maxLastInstanceN = nextInstanceN;
			}
			put(serverAddr, loadBuilderSvc);
		}
		// set properties should be invoked only after the map is filled already
		setProperties(runTimeConfig);
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
		if(reqConf == null) {
			throw new IllegalStateException("Shared request config is not initialized");
		} else {
			reqConf.setProperties(runTimeConfig);
		}
		//
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			LOG.debug(Markers.MSG, "Applying the configuration to server @ \"{}\"...", addr);
			nextBuilder.setProperties(runTimeConfig);
		}
		//
		final String newAddrs[] = runTimeConfig.getStorageAddrsWithPorts();
		if(newAddrs != null && newAddrs.length > 0) {
			dataNodeAddrs = newAddrs;
		}
		//
		String dataMetaInfoFile = null;
		try {
			dataMetaInfoFile = this.runTimeConfig.getDataSrcFPath();
			if(
				dataMetaInfoFile!=null && dataMetaInfoFile.length() > 0 &&
				Files.isReadable(Paths.get(dataMetaInfoFile))
			) {
				setInputFile(dataMetaInfoFile);
				// disable API-specific producers
				reqConf.setAnyDataProducerEnabled(false);
				// disable file-based producers on the load servers side
				for(final LoadBuilderSvc<T, U> nextLoadBuilder : values()) {
					nextLoadBuilder.setInputFile(null);
				}
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
	public final LoadBuilderClient<T, U> setLoadType(final IOTask.Type loadType)
	throws IllegalStateException, RemoteException {
		reqConf.setLoadType(loadType);
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
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
		for(final String addr : keySet()) {
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
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMinObjSize(minObjSize);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setObjSizeBias(final float objSizeBias)
		throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setObjSizeBias(objSizeBias);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMaxObjSize(maxObjSize);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setRateLimit(final float rateLimit)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRateLimit(rateLimit);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setThreadsPerNodeDefault(final short threadCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadsPerNodeDefault(threadCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setThreadsPerNodeFor(
		final short threadCount, final IOTask.Type loadType
	) throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadsPerNodeFor(threadCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		if(dataNodeAddrs != null && dataNodeAddrs.length > 0) {
			this.dataNodeAddrs = dataNodeAddrs;
			//
			LoadBuilderSvc<T, U> nextBuilder;
			for(final String addr : keySet()) {
				nextBuilder = get(addr);
				nextBuilder.setDataNodeAddrs(dataNodeAddrs);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setUpdatesPerItem(int count)
	throws RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
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
	public final U build()
	throws RemoteException {
		try {
			invokePreConditions();
		} catch(final IllegalStateException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Preconditions failure");
		}
		return buildActually();
	}
	//
	protected abstract void invokePreConditions()
	throws IllegalStateException;
	//
	protected abstract U buildActually()
	throws RemoteException;
	//
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder(reqConf.toString());
		try {
			strBuilder.append('-').append(get(keySet().iterator().next()).getLastInstanceNum());
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to make load builder string");
		}
		return strBuilder.toString();
	}
	//
	@Override
	public final void close()
	throws IOException {
		reqConf.close();
	}
}
