package com.emc.mongoose.client.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.builder.LoadBuilderClient;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
//
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	protected String listFile = null, nodeAddrs[] = null, loadServerAddrs[] = null;
	protected volatile RunTimeConfig runTimeConfig;
	protected volatile RequestConfig<T> reqConf = getDefaultRequestConfig();
	protected long maxCount = 0, minObjSize, maxObjSize;
	protected float objSizeBias;
	//
	protected boolean flagAssignLoadSvcToNode = false;
	protected final Map<String, RunTimeConfig> loadSvcConfMap = new HashMap<>();
	//
	public LoadBuilderClientBase()
	throws IOException {
		this(RunTimeConfig.getContext());
	}
	//
	public LoadBuilderClientBase(final RunTimeConfig runTimeConfig)
	throws IOException {
		//
		super(runTimeConfig.getLoadServerAddrs().length);
		loadServerAddrs = runTimeConfig.getLoadServerAddrs();
		//
		LoadBuilderSvc<T, U> loadBuilderSvc;
		int maxLastInstanceN = 0, nextInstanceN;
		for(final String serverAddr : loadServerAddrs) {
			LOG.info(Markers.MSG, "Resolving service @ \"{}\"...", serverAddr);
			loadBuilderSvc = resolve(serverAddr);
			nextInstanceN = loadBuilderSvc.getNextInstanceNum(runTimeConfig.getRunId());
			if(nextInstanceN > maxLastInstanceN) {
				maxLastInstanceN = nextInstanceN;
			}
			put(serverAddr, loadBuilderSvc);
		}
		// set properties should be invoked only after the map is filled already
		setProperties(runTimeConfig);
		//
		for(final String serverAddr : loadServerAddrs) {
			get(serverAddr).setNextInstanceNum(runTimeConfig.getRunId(), maxLastInstanceN);
		}
	}
	//
	protected abstract RequestConfig<T> getDefaultRequestConfig();
	//
	protected abstract LoadBuilderSvc<T, U> resolve(final String serverAddr)
	throws IOException;
	//
	private int greatestCommonDivider(final int x, final int y) {
		int z = 0;
		if(x > y) {
			for(int i = y; i > 0; i --) {
				if(x % i == 0 && y % i == 0) {
					z = i;
					break;
				}
			}
		} else if(x < y) {
			for(int i = x; i > 0; i --) {
				if(x % i == 0 && y % i == 0) {
					z = i;
					break;
				}
			}
		} else {
			z = x;
		}
		return z;
	}
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
		final String newNodeAddrs[] = runTimeConfig.getStorageAddrsWithPorts();
		if(newNodeAddrs.length > 0) {
			nodeAddrs = newNodeAddrs;
		}
		flagAssignLoadSvcToNode = runTimeConfig.getFlagAssignLoadServerToNode();
		if(flagAssignLoadSvcToNode) {
			final int
				countNodes = nodeAddrs.length,
				countLoadSvcs = loadServerAddrs.length;
			if(countLoadSvcs > 1 || countNodes > 1) {
				final int k = greatestCommonDivider(countNodes, countLoadSvcs);
				if(k > 0) {
					final int
						countLoadSvcPerSameNodeList = countLoadSvcs / k,
						countNodesPerLoadSvc = countNodes / k;
					RunTimeConfig nextConfig;
					String nextLoadSvcAddr, nextNodeAddrs;
					List<String> nextNodeAddrList;
					for(int i = 0; i < k; i ++) {
						nextNodeAddrList = new ArrayList<>(countNodesPerLoadSvc);
						for(int j = 0; j < countNodesPerLoadSvc; j ++) {
							nextNodeAddrList.add(
								nodeAddrs[i * countNodesPerLoadSvc + j]
							);
						}
						nextNodeAddrs = StringUtils.join(nextNodeAddrList, ',');
						for(int j = 0; j < countLoadSvcPerSameNodeList; j ++) {
							nextLoadSvcAddr = loadServerAddrs[
								i * countLoadSvcPerSameNodeList + j
							];
							nextConfig = loadSvcConfMap.get(nextLoadSvcAddr);
							if(nextConfig == null) {
								nextConfig = (RunTimeConfig) runTimeConfig.clone();
								loadSvcConfMap.put(nextLoadSvcAddr, nextConfig);
							}
							LOG.info(
								Markers.MSG, "Next assingment: " + nextLoadSvcAddr + " <-> " +
								nextNodeAddrs
							);
							nextConfig.setProperty(RunTimeConfig.KEY_STORAGE_ADDRS, newNodeAddrs);
						}
					}
				} else {
					throw new IllegalStateException(
						"Failed to calculate the greatest common divider for the count of the " +
						"load servers (" + countLoadSvcs + ") and the count of the storage nodes " +
						"(" + countNodes + ")"
					);
				}
			}
		}
		//
		LoadBuilderSvc<T, U> nextBuilder;
		RunTimeConfig nextLoadSvcConfig;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextLoadSvcConfig = loadSvcConfMap.get(addr);
			if(nextLoadSvcConfig == null) {
				nextLoadSvcConfig = runTimeConfig; // use default
				LOG.debug(
					Markers.MSG, "Applying the common configuration to server @ \"{}\"...", addr
				);
			} else {
				LOG.debug(
					Markers.MSG, "Applying the specific configuration to server @ \"{}\"...", addr
				);
			}
			nextBuilder.setProperties(nextLoadSvcConfig);
		}
		//
		setMaxCount(runTimeConfig.getLoadLimitCount());
		setMinObjSize(runTimeConfig.getDataSizeMin());
		setMaxObjSize(runTimeConfig.getDataSizeMax());
		setObjSizeBias(runTimeConfig.getDataSizeBias());
		//
		try {
			final String listFile = this.runTimeConfig.getDataSrcFPath();
			if(listFile != null && listFile.length() > 0) {
				final Path dataItemsListPath = Paths.get(listFile);
				if(!Files.exists(dataItemsListPath)) {
					LOG.warn(
						Markers.ERR, "Data items source file \"{}\" doesn't exist",
						dataItemsListPath
					);
				} else if(!Files.isReadable(dataItemsListPath)) {
					LOG.warn(
						Markers.ERR, "Data items source file \"{}\" is not readable",
						dataItemsListPath
					);
				} else {
					setInputFile(listFile);
					// disable API-specific producers
					reqConf.setContainerInputEnabled(false);
					// disable file-based producers on the load servers side
					for(final LoadBuilderSvc<T, U> nextLoadBuilder : values()) {
						nextLoadBuilder.setInputFile(null);
					}
				}
			}
		} catch(final NoSuchElementException e) {
			LOG.warn(Markers.ERR, "No \"data.src.fpath\" property available");
		} catch(final InvalidPathException e) {
			LOG.warn(Markers.ERR, "Invalid data metainfo src file path: {}", listFile);
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
		if(this.reqConf.equals(reqConf)) {
			return this;
		}
		try {
			this.reqConf.close(); // see jira ticket #437
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to close the replacing req config instance #{}",
				hashCode()
			);
		}
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
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
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
		this.minObjSize = minObjSize;
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
		this.objSizeBias = objSizeBias;
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
		this.maxObjSize = maxObjSize;
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
	public final LoadBuilderClient<T, U> setThreadCountDefault(final int threadCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadCountDefault(threadCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setThreadCountFor(
		final int threadCount, final IOTask.Type loadType
	) throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadCountFor(threadCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setConnPerNodeDefault(final int connCount)
		throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadCountDefault(connCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setConnPerNodeFor(
		final int connCount, final IOTask.Type loadType
	) throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setThreadCountFor(connCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		if(dataNodeAddrs != null && dataNodeAddrs.length > 0) {
			this.nodeAddrs = dataNodeAddrs;
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
		return this;
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
		U client = null;
		try {
			invokePreConditions();
			client = buildActually();
		} catch (final DuplicateSvcNameException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Possible load service usage collision");
		} catch(final IllegalStateException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Preconditions failure");
		}
		return client;
	}
	//
	protected abstract void invokePreConditions()
	throws IllegalStateException;
	//
	protected abstract U buildActually()
	throws RemoteException, DuplicateSvcNameException;
	//
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder(reqConf.toString());
		try {
			strBuilder.append('-').append(get(keySet().iterator().next())
				.getNextInstanceNum(runTimeConfig.getRunId()));
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
