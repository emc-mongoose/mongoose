package com.emc.mongoose.client.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.math.MathUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.api.load.builder.LoadBuilderClient;
//
import com.emc.mongoose.core.impl.data.model.ItemCSVFileSrc;
// mongoose-server-api.jar
import com.emc.mongoose.core.impl.load.builder.LoadBuilderBase;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.LoadSvc;
//
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
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
extends LoadBuilderBase<T, U>
implements LoadBuilderClient<T, W, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final String loadSvcAddrs[];
	//
	protected boolean flagAssignLoadSvcToNode = false;
	protected final Map<String, V> loadSvcMap = new HashMap<>();
	protected final Map<String, RunTimeConfig> loadSvcConfMap = new HashMap<>();
	//
	protected LoadBuilderClientBase()
	throws IOException {
		this(RunTimeConfig.getContext());
	}
	//
	protected LoadBuilderClientBase(final RunTimeConfig rtConfig)
	throws IOException {
		super(rtConfig);
		loadSvcAddrs = rtConfig.getLoadServerAddrs();
		V loadBuilderSvc;
		int maxLastInstanceN = 0, nextInstanceN;
		for(final String serverAddr : loadSvcAddrs) {
			try {
				loadBuilderSvc = resolve(serverAddr);
				LOG.info(
						Markers.MSG, "Resolved service \"{}\" @ {}",
						loadBuilderSvc.getName(), serverAddr
				);
				nextInstanceN = loadBuilderSvc.getNextInstanceNum(rtConfig.getRunId());
				if(nextInstanceN > maxLastInstanceN) {
					maxLastInstanceN = nextInstanceN;
				}
				loadSvcMap.put(serverAddr, loadBuilderSvc);
				loadSvcConfMap.put(serverAddr, (RunTimeConfig)rtConfig.clone());
			} catch(final RemoteException e) {
				LogUtil.exception(
						LOG, Level.ERROR, e, "Failed to lock load builder service @ {}", serverAddr
				);
			}
		}
		//
		resetItemSrc();
		// set properties should be invoked only after the map is filled already
		setRunTimeConfig(rtConfig);
		//
		for(final String serverAddr : loadSvcAddrs) {
			loadSvcMap.get(serverAddr).setNextInstanceNum(rtConfig.getRunId(), maxLastInstanceN);
		}
	}
	//
	protected abstract IOConfig<?, ?> getDefaultIOConfig();
	//
	protected abstract V resolve(final String serverAddr)
	throws IOException;
	//
	protected static void assignNodesToLoadSvcs(
		final Map<String, RunTimeConfig> dstConfMap,
		final String loadSvcAddrs[], final String nodeAddrs[]
	) throws IllegalStateException {
		if(loadSvcAddrs != null && (loadSvcAddrs.length > 1 || nodeAddrs.length > 1)) {
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
	public LoadBuilderClient<T, W, U> setRunTimeConfig(final RunTimeConfig rtConfig)
	throws IllegalStateException, RemoteException {
		//
		super.setRunTimeConfig(rtConfig);
		//
		final String newNodeAddrs[] = rtConfig.getStorageAddrsWithPorts();
		if(newNodeAddrs.length > 0) {
			storageNodeAddrs = newNodeAddrs;
		}
		flagAssignLoadSvcToNode = rtConfig.getFlagAssignLoadServerToNode();
		if(flagAssignLoadSvcToNode) {
			assignNodesToLoadSvcs(loadSvcConfMap, loadSvcAddrs, storageNodeAddrs);
		}
		//
		V nextBuilder;
		RunTimeConfig nextLoadSvcConfig;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
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
				nextBuilder.setRunTimeConfig(nextLoadSvcConfig);
			}
		}
		//
		setMaxCount(rtConfig.getLoadLimitCount());
		setRateLimit(rtConfig.getLoadLimitRate());
		setManualTaskSleepMicroSecs(
			(int) TimeUnit.MILLISECONDS.toMicros(rtConfig.getLoadLimitReqSleepMilliSec())
		);
		//
		try {
			final String listFile = rtConfig.getItemSrcFile();
			if(itemsFileExists(listFile) && loadSvcMap != null) {
				setItemSrc(
					new ItemCSVFileSrc<>(
						Paths.get(listFile), (Class<T>) ioConfig.getItemClass(),
						ioConfig.getContentSource()
					)
				);
				// disable file-based item sources on the load servers side
				for(final V nextLoadBuilder : loadSvcMap.values()) {
					nextLoadBuilder.setItemSrc(null);
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
	public final LoadBuilderClient<T, W, U> setLoadType(final IOTask.Type loadType)
	throws IllegalStateException, RemoteException {
		super.setLoadType(loadType);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setLoadType(loadType);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException {
		super.setMaxCount(maxCount);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setMaxCount(maxCount);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setManualTaskSleepMicroSecs(
		final int manualTaskSleepMicroSecs
	) throws IllegalArgumentException, RemoteException {
		super.setManualTaskSleepMicroSecs(manualTaskSleepMicroSecs);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setRateLimit(manualTaskSleepMicroSecs);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setRateLimit(final float rateLimit)
	throws IllegalArgumentException, RemoteException {
		super.setRateLimit(rateLimit);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setRateLimit(rateLimit);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setWorkerCountDefault(final int threadCount)
	throws IllegalArgumentException, RemoteException {
		super.setWorkerCountDefault(threadCount);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setWorkerCountDefault(threadCount);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setWorkerCountFor(
		final int threadCount, final IOTask.Type loadType
	) throws IllegalArgumentException, RemoteException {
		super.setWorkerCountFor(threadCount, loadType);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setWorkerCountFor(threadCount, loadType);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setConnPerNodeDefault(final int connCount)
	throws IllegalArgumentException, RemoteException {
		super.setConnPerNodeDefault(connCount);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setConnPerNodeDefault(connCount);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setConnPerNodeFor(
		final int connCount, final IOTask.Type loadType
	) throws IllegalArgumentException, RemoteException {
		super.setConnPerNodeFor(connCount, loadType);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setConnPerNodeFor(connCount, loadType);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		super.setDataNodeAddrs(dataNodeAddrs);
		if(dataNodeAddrs != null && dataNodeAddrs.length > 0) {
			this.storageNodeAddrs = dataNodeAddrs;
			if(flagAssignLoadSvcToNode) {
				assignNodesToLoadSvcs(loadSvcConfMap, loadSvcAddrs, storageNodeAddrs);
			}
			//
			V nextBuilder;
			if(loadSvcMap != null) {
				for(final String addr : loadSvcMap.keySet()) {
					nextBuilder = loadSvcMap.get(addr);
					nextBuilder.setDataNodeAddrs(
						loadSvcConfMap.get(addr).getStorageAddrs()
					);
				}
			}
		}
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public LoadBuilderClient<T, W, U> setItemSrc(final ItemSrc<T> itemSrc)
	throws RemoteException {
		super.setItemSrc(itemSrc);
		if(itemSrc != null && loadSvcMap != null) {
			// disable any item source usage on the load servers side
			V nextBuilder;
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.useNoneItemSrc();
			}
		}
		return this;
	}
	//
	protected abstract ItemSrc<T> getDefaultItemSource();
	//
	protected void resetItemSrc() {
		flagUseNewItemSrc = true;
		flagUseNoneItemSrc = false;
		itemSrc = null;
	}
	//
	@Override
	public String toString() {
		final StringBuilder strBuilder = new StringBuilder(ioConfig.toString());
		try {
			strBuilder.append('.').append(loadSvcMap.get(loadSvcMap.keySet().iterator().next())
				.getNextInstanceNum(rtConfig.getRunId()));
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to make load builder string");
		}
		return strBuilder.toString();
	}
	//
	@Override
	public void close()
	throws IOException {
		V nextLoadBuilderSvc;
		for(final String loadSvcAddr : loadSvcAddrs) {
			nextLoadBuilderSvc = loadSvcMap.get(loadSvcAddr);
			if(nextLoadBuilderSvc != null) {
				try {
					nextLoadBuilderSvc.close();
				} catch(final IOException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to close the load builder service @ {}",
						loadSvcAddr
					);
				}
			}
		}
	}
}
