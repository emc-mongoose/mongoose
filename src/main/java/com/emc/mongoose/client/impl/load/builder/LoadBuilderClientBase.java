package com.emc.mongoose.client.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.math.MathUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.req.RequestConfig;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.api.load.builder.LoadBuilderClient;
//
import com.emc.mongoose.core.impl.data.model.CSVFileItemSrc;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
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
public abstract class LoadBuilderClientBase<
	T extends Item,
	W extends LoadSvc<T>,
	U extends LoadClient<T, W>,
	V extends LoadBuilderSvc<T, W>
>
extends HashMap<String, V>
implements LoadBuilderClient<T, W, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected ItemSrc<T> itemSrc;
	protected String storageNodeAddrs[] = null, loadSvcAddrs[] = null;
	protected volatile RunTimeConfig rtConfig;
	protected volatile RequestConfig<T> reqConf = getDefaultRequestConfig();
	protected long maxCount = 0;
	//
	protected boolean
		flagAssignLoadSvcToNode = false,
		flagUseNewItemSrc, flagUseNoneItemSrc;
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
		V loadBuilderSvc;
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
		//
		resetItemSrc();
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
	protected abstract V resolve(final String serverAddr)
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
	public LoadBuilderClient<T, W, U> setProperties(final RunTimeConfig rtConfig)
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
		V nextBuilder;
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
		setRateLimit(rtConfig.getLoadLimitRate());
		setManualTaskSleepMicroSecs(
			(int) TimeUnit.MILLISECONDS.toMicros(rtConfig.getLoadLimitReqSleepMilliSec())
		);
		//
		try {
			final String listFile = this.rtConfig.getItemSrcFPath();
			if(listFile != null && !listFile.isEmpty()) {
				final Path itemsListPath = Paths.get(listFile);
				if(!Files.exists(itemsListPath)) {
					LOG.warn(
						Markers.ERR, "Data items source file \"{}\" doesn't exist",
						itemsListPath
					);
				} else if(!Files.isReadable(itemsListPath)) {
					LOG.warn(
						Markers.ERR, "Data items source file \"{}\" is not readable",
						itemsListPath
					);
				} else if(Files.isDirectory(itemsListPath)) {
					LOG.warn(
						Markers.ERR, "Data items source file \"{}\" is a directory",
						itemsListPath
					);
				} else {
					setItemSrc(
						new CSVFileItemSrc<>(
							itemsListPath, reqConf.getItemClass(), reqConf.getContentSource()
						)
					);
					// disable file-based item sources on the load servers side
					for(final V nextLoadBuilder : values()) {
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
	public final LoadBuilderClient<T, W, U> setRequestConfig(final RequestConfig<T> reqConf)
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
	public final LoadBuilderClient<T, W, U> setLoadType(final IOTask.Type loadType)
	throws IllegalStateException, RemoteException {
		reqConf.setLoadType(loadType);
		V nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setLoadType(loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException {
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		V nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setMaxCount(maxCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setManualTaskSleepMicroSecs(
		final int manualTaskSleepMicroSecs
	) throws IllegalArgumentException, RemoteException {
		V nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRateLimit(manualTaskSleepMicroSecs);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setRateLimit(final float rateLimit)
	throws IllegalArgumentException, RemoteException {
		V nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setRateLimit(rateLimit);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setWorkerCountDefault(final int threadCount)
	throws IllegalArgumentException, RemoteException {
		V nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setWorkerCountDefault(threadCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setWorkerCountFor(
		final int threadCount, final IOTask.Type loadType
	) throws IllegalArgumentException, RemoteException {
		V nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setWorkerCountFor(threadCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setConnPerNodeDefault(final int connCount)
	throws IllegalArgumentException, RemoteException {
		V nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setConnPerNodeDefault(connCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setConnPerNodeFor(
		final int connCount, final IOTask.Type loadType
	) throws IllegalArgumentException, RemoteException {
		V nextBuilder;
		for(final String addr : keySet()) {
			nextBuilder = get(addr);
			nextBuilder.setConnPerNodeFor(connCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		if(dataNodeAddrs != null && dataNodeAddrs.length > 0) {
			this.storageNodeAddrs = dataNodeAddrs;
			if(flagAssignLoadSvcToNode) {
				assignNodesToLoadSvcs(rtConfig, loadSvcConfMap, loadSvcAddrs, storageNodeAddrs);
			}
			//
			V nextBuilder;
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
	public LoadBuilderClient<T, W, U> useNewItemSrc()
	throws RemoteException {
		flagUseNewItemSrc = true;
		return this;
	}
	//
	@Override
	public LoadBuilderClient<T, W, U> useNoneItemSrc()
	throws RemoteException {
		flagUseNoneItemSrc = true;
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public LoadBuilderClient<T, W, U> setItemSrc(final ItemSrc<T> itemSrc)
	throws RemoteException {
		LOG.debug(Markers.MSG, "Set data items source: {}", itemSrc);
		this.itemSrc = itemSrc;
		if(itemSrc != null) {
			// disable any item source usage on the load servers side
			V nextBuilder;
			for(final String addr : keySet()) {
				nextBuilder = get(addr);
				nextBuilder.useNoneItemSrc();
			}
		}
		return this;
	}
	//
	protected abstract ItemSrc<T> getDefaultItemSource();
	//
	protected final void resetItemSrc() {
		flagUseNewItemSrc = true;
		flagUseNoneItemSrc = false;
		itemSrc = null;
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
		} finally {
			resetItemSrc();
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
