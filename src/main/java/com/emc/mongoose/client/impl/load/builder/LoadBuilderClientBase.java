package com.emc.mongoose.client.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.math.MathUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.data.model.FileDataItemSrc;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.builder.LoadBuilderClient;
// mongoose-server-api.jar
import com.emc.mongoose.core.impl.data.model.CSVFileItemSrc;
import com.emc.mongoose.core.impl.data.model.NewDataItemSrc;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
//
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 20.10.14.
 */
public abstract class LoadBuilderClientBase<T extends DataItem, U extends LoadExecutor<T>>
extends HashMap<String, LoadBuilderSvc<T, U>>
implements LoadBuilderClient<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected DataItemSrc<T> itemSrc;
	protected String storageNodeAddrs[] = null, loadSvcAddrs[] = null;
	protected volatile RunTimeConfig rtConfig;
	protected volatile RequestConfig<T> reqConf = getDefaultRequestConfig();
	protected long maxCount = 0, minObjSize, maxObjSize;
	protected float objSizeBias;
	//
	protected boolean
		flagAssignLoadSvcToNode = false,
		flagUseContainerItemSrc = true,
		flagUseNewItemSrc = true,
		flagUseNoneItemSrc = false;
	protected final Map<String, RunTimeConfig> loadSvcConfMap = new HashMap<>();
	//
	public LoadBuilderClientBase()
	throws IOException {
		this(RunTimeConfig.getContext());
	}
	//
	public LoadBuilderClientBase(final RunTimeConfig rtConfig)
	throws IOException {
		//
		super(rtConfig.getLoadServerAddrs().length);
		loadSvcAddrs = rtConfig.getLoadServerAddrs();
		//
		LoadBuilderSvc<T, U> loadBuilderSvc;
		int maxLastInstanceN = 0, nextInstanceN;
		for(final String serverAddr : loadSvcAddrs) {
			LOG.info(Markers.MSG, "Resolving service @ \"{}\"...", serverAddr);
			loadBuilderSvc = resolve(serverAddr);
			nextInstanceN = loadBuilderSvc.getNextInstanceNum(rtConfig.getRunId());
			if(nextInstanceN > maxLastInstanceN) {
				maxLastInstanceN = nextInstanceN;
			}
			put(serverAddr, loadBuilderSvc);
		}
		// set properties should be invoked only after the map is filled already
		setProperties(rtConfig);
		//
		for(final String serverAddr : loadSvcAddrs) {
			get(serverAddr).setNextInstanceNum(rtConfig.getRunId(), maxLastInstanceN);
		}
	}
	//
	protected abstract RequestConfig<T> getDefaultRequestConfig();
	//
	protected abstract LoadBuilderSvc<T, U> resolve(final String serverAddr)
	throws IOException;
	//
	protected static void assignNodesToLoadSvcs(
		final RunTimeConfig srcConf, final Map<String, RunTimeConfig> dstConfMap,
		final String loadSvcAddrs[], final String nodeAddrs[]
	) throws IllegalStateException {
		if(loadSvcAddrs.length > 1 || nodeAddrs.length > 1) {
			final int nStep = MathUtil.gcd(loadSvcAddrs.length, nodeAddrs.length);
			if(nStep > 0) {
				final int
					nLoadSvcPerStep = loadSvcAddrs.length / nStep,
					nNodesPerStep = nodeAddrs.length / nStep;
				RunTimeConfig nextConfig;
				String nextLoadSvcAddr, nextNodeAddrs;
				int j;
				for(int i = 0; i < nStep; i ++) {
					//
					j = i * nNodesPerStep;
					nextNodeAddrs = StringUtils.join(
						Arrays.asList(nodeAddrs).subList(j, j + nNodesPerStep), ','
					);
					//
					for(j = 0; j < nLoadSvcPerStep; j ++) {
						nextLoadSvcAddr = loadSvcAddrs[i * nLoadSvcPerStep + j];
						nextConfig = dstConfMap.get(nextLoadSvcAddr);
						if(nextConfig == null) {
							nextConfig = (RunTimeConfig) srcConf.clone();
							dstConfMap.put(nextLoadSvcAddr, nextConfig);
						}
						LOG.info(
							Markers.MSG, "Load server @ " + nextLoadSvcAddr +
							" will use the following storage nodes: " + nextNodeAddrs
						);
						nextConfig.setProperty(RunTimeConfig.KEY_STORAGE_ADDRS, nextNodeAddrs);
					}
				}
			} else {
				throw new IllegalStateException(
					"Failed to calculate the greatest common divider for the count of the " +
					"load servers (" + loadSvcAddrs.length + ") and the count of the storage " +
					"nodes (" + nodeAddrs.length + ")"
				);
			}
		}
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setProperties(final RunTimeConfig rtConfig)
	throws IllegalStateException, RemoteException {
		//
		this.rtConfig = rtConfig;
		if(reqConf == null) {
			throw new IllegalStateException("Shared request config is not initialized");
		} else {
			reqConf.setProperties(rtConfig);
		}
		//
		final String newNodeAddrs[] = rtConfig.getStorageAddrsWithPorts();
		if(newNodeAddrs.length > 0) {
			storageNodeAddrs = newNodeAddrs;
		}
		flagAssignLoadSvcToNode = rtConfig.getFlagAssignLoadServerToNode();
		if(flagAssignLoadSvcToNode) {
			assignNodesToLoadSvcs(rtConfig, loadSvcConfMap, loadSvcAddrs, storageNodeAddrs);
		}
		//
		LoadBuilderSvc<T, U> nextBuilder;
		RunTimeConfig nextLoadSvcConfig;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextLoadSvcConfig = loadSvcConfMap.get(addr);
			if(nextLoadSvcConfig == null) {
				nextLoadSvcConfig = rtConfig; // use default
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
		setMaxCount(rtConfig.getLoadLimitCount());
		setMinObjSize(rtConfig.getDataSizeMin());
		setMaxObjSize(rtConfig.getDataSizeMax());
		setObjSizeBias(rtConfig.getDataSizeBias());
		setRateLimit(rtConfig.getLoadLimitRate());
		setManualTaskSleepMicroSecs(
			(int) TimeUnit.MILLISECONDS.toMicros(rtConfig.getLoadLimitReqSleepMilliSec())
		);
		//
		try {
			final String listFile = this.rtConfig.getDataSrcFPath();
			if(listFile != null && !listFile.isEmpty()) {
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
				} else if(Files.isDirectory(dataItemsListPath)) {
					LOG.warn(
						Markers.ERR, "Data items source file \"{}\" is a directory",
						dataItemsListPath
					);
				} else {
					setItemSrc(new CSVFileItemSrc<>(dataItemsListPath, reqConf.getDataItemClass()));
					// disable file-based item sources on the load servers side
					for(final LoadBuilderSvc<T, U> nextLoadBuilder : values()) {
						nextLoadBuilder.setItemSrc(null);
					}
				}
			}
		} catch(final NoSuchElementException e) {
			LOG.warn(Markers.ERR, "No \"data.src.fpath\" value was set");
		} catch(final IOException e) {
			LOG.warn(Markers.ERR, "Invalid items source file path: {}", itemSrc);
		} catch(final SecurityException | NoSuchMethodException e) {
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
	public final LoadBuilderClient<T, U> setManualTaskSleepMicroSecs(
		final int manualTaskSleepMicroSecs
	) throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRateLimit(manualTaskSleepMicroSecs);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setRateLimit(final float rateLimit)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRateLimit(rateLimit);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setWorkerCountDefault(final int threadCount)
	throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setWorkerCountDefault(threadCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setWorkerCountFor(
		final int threadCount, final IOTask.Type loadType
	) throws IllegalArgumentException, RemoteException {
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setWorkerCountFor(threadCount, loadType);
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
			nextBuilder.setConnPerNodeDefault(connCount);
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
			nextBuilder.setConnPerNodeFor(connCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		if(dataNodeAddrs != null && dataNodeAddrs.length > 0) {
			this.storageNodeAddrs = dataNodeAddrs;
			if(flagAssignLoadSvcToNode) {
				assignNodesToLoadSvcs(rtConfig, loadSvcConfMap, loadSvcAddrs, storageNodeAddrs);
			}
			//
			LoadBuilderSvc<T, U> nextBuilder;
			for(final String addr : keySet()) {
				nextBuilder = get(addr);
				nextBuilder.setDataNodeAddrs(
					loadSvcConfMap.get(addr).getStorageAddrs()
				);
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
	//
	@Override
	public LoadBuilderClientBase<T, U> useNewItemSrc()
	throws RemoteException {
		// disable new data item generation on the client side
		flagUseNewItemSrc = true;
		// enable new data item generation on the load servers side
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.useNewItemSrc();
		}
		return this;
	}
	//
	@Override
	public LoadBuilderClientBase<T, U> useNoneItemSrc()
	throws RemoteException {
		// disable any item source usage on the client side
		flagUseNoneItemSrc = true;
		// disable any item source usage on the load servers side
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.useNoneItemSrc();
		}
		return this;
	}
	//
	@Override
	public LoadBuilderClientBase<T, U> useContainerListingItemSrc()
	throws RemoteException {
		// enable container listing item source on the client sid
		flagUseContainerItemSrc = true;
		// disable any item source usage on the load servers side
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.useNoneItemSrc();
		}
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final LoadBuilderClient<T, U> setItemSrc(final DataItemSrc<T> itemSrc)
	throws RemoteException {
		LOG.debug(Markers.MSG, "Set data items source: {}", itemSrc);
		this.itemSrc = itemSrc;
		// disable any item source usage on the load servers side
		LoadBuilderSvc<T, U> nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.useNoneItemSrc();
		}
		//
		if(itemSrc instanceof FileDataItemSrc) {
			// calculate approx average data item size
			final FileDataItemSrc<T> fileInput = (FileDataItemSrc<T>) itemSrc;
			final long approxDataItemsSize = fileInput.getApproxDataItemsSize(
				rtConfig.getBatchSize()
			);
			reqConf.setBuffSize(
				approxDataItemsSize < Constants.BUFF_SIZE_LO ?
					Constants.BUFF_SIZE_LO :
					approxDataItemsSize > Constants.BUFF_SIZE_HI ?
						Constants.BUFF_SIZE_HI : (int) approxDataItemsSize
			);
		}
		return this;
	}
	//
	protected DataItemSrc<T> getDefaultItemSource() {
		try {
			if(flagUseNoneItemSrc) {
				return null;
			} else if(flagUseContainerItemSrc && flagUseNewItemSrc) {
				if(IOTask.Type.CREATE.equals(reqConf.getLoadType())) {
					return new NewDataItemSrc<>(
						reqConf.getDataItemClass(), minObjSize, maxObjSize, objSizeBias
					);
				} else {
					return reqConf.getContainerListInput(maxCount, storageNodeAddrs[0]);
				}
			} else if(flagUseNewItemSrc) {
				return new NewDataItemSrc<>(
					reqConf.getDataItemClass(), minObjSize, maxObjSize, objSizeBias
				);
			} else if(flagUseContainerItemSrc) {
				return reqConf.getContainerListInput(maxCount, storageNodeAddrs[0]);
			}
		} catch(final NoSuchMethodException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to build the new data items source");
		}
		return null;
	}
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
				.getNextInstanceNum(rtConfig.getRunId()));
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
