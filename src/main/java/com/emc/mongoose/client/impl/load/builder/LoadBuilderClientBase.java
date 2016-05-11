package com.emc.mongoose.client.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.math.MathUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.item.base.Item;
// mongoose-client.jar
import com.emc.mongoose.client.api.load.executor.LoadClient;
import com.emc.mongoose.client.api.load.builder.LoadBuilderClient;
//
import com.emc.mongoose.core.impl.item.base.ItemCsvFileOutput;
import com.emc.mongoose.core.impl.item.base.CsvFileItemInput;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
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
	protected final Map<String, AppConfig> loadSvcConfMap = new HashMap<>();
	//
	protected LoadBuilderClientBase()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	protected LoadBuilderClientBase(final AppConfig appConfig)
	throws IOException {
		super(appConfig);
		loadSvcAddrs = appConfig.getLoadServerAddrs();
		V loadBuilderSvc;
		int maxLastInstanceN = 0, nextInstanceN;
		for(final String serverAddr : loadSvcAddrs) {
			try {
				loadBuilderSvc = resolve(serverAddr);
				LOG.info(
					Markers.MSG, "Resolved service \"{}\" @ {}",
					loadBuilderSvc.getName(), serverAddr
				);
				nextInstanceN = loadBuilderSvc.getNextInstanceNum(appConfig.getRunId());
				if(nextInstanceN > maxLastInstanceN) {
					maxLastInstanceN = nextInstanceN;
				}
				loadSvcMap.put(serverAddr, loadBuilderSvc);
				loadSvcConfMap.put(serverAddr, (AppConfig) appConfig.clone());
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to lock load builder service @ {}", serverAddr
				);
			} catch(final CloneNotSupportedException e ) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the configuration");
			}
		}
		// set properties should be invoked only after the map is filled already
		setAppConfig(appConfig);
		//
		for(final String serverAddr : loadSvcAddrs) {
			loadSvcMap.get(serverAddr).setNextInstanceNum(appConfig.getRunId(), maxLastInstanceN);
		}
	}
	//
	protected abstract V resolve(final String serverAddr)
	throws IOException;
	//
	protected static void assignNodesToLoadSvcs(
		final Map<String, AppConfig> dstConfMap,
		final String loadSvcAddrs[], final String nodeAddrs[]
	) throws IllegalStateException {
		if(loadSvcAddrs != null && (loadSvcAddrs.length > 1 || nodeAddrs.length > 1)) {
			final int nStep = MathUtil.gcd(loadSvcAddrs.length, nodeAddrs.length);
			if(nStep > 0) {
				final int
					nLoadSvcPerStep = loadSvcAddrs.length / nStep,
					nNodesPerStep = nodeAddrs.length / nStep;
				AppConfig nextConfig;
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
						nextConfig.setProperty(AppConfig.KEY_STORAGE_ADDRS, nextNodeAddrs);
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
	public LoadBuilderClient<T, W, U> setAppConfig(final AppConfig appConfig)
	throws IllegalStateException, RemoteException {
		//
		super.setAppConfig(appConfig);
		//
		storageNodeAddrs = appConfig.getStorageAddrsWithPorts();
		flagAssignLoadSvcToNode = appConfig.getLoadServerNodeMapping();
		if(flagAssignLoadSvcToNode) {
			assignNodesToLoadSvcs(loadSvcConfMap, loadSvcAddrs, storageNodeAddrs);
		}
		//
		V nextBuilder;
		AppConfig nextLoadSvcConfig;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextLoadSvcConfig = loadSvcConfMap.get(addr);
				if(nextLoadSvcConfig == null) {
					nextLoadSvcConfig = appConfig; // use default
					LOG.debug(
						Markers.MSG, "Applying the common configuration to server @ \"{}\"...", addr
					);
				} else {
					LOG.debug(
						Markers.MSG, "Applying the specific configuration to server @ \"{}\"...",
						addr
					);
				}
				try {
					nextBuilder.setAppConfig(nextLoadSvcConfig);
				} catch(final Exception e) {
					LogUtil.exception(
						LOG, Level.ERROR, e,
						"Failed to apply the configuration to the server @ \"{}\"", addr
					);
				}
			}
		}
		//
		setCountLimit(appConfig.getLoadLimitCount());
		setSizeLimit(appConfig.getLoadLimitSize());
		setRateLimit((float) appConfig.getLoadLimitRate());
		//
		try {
			final String listFile = appConfig.getItemSrcFile();
			if(itemsFileExists(listFile) && loadSvcMap != null) {
				setInput(
					new CsvFileItemInput<>(
						Paths.get(listFile), (Class<T>) ioConfig.getItemClass(),
						ioConfig.getContentSource()
					)
				);
				// disable file-based item sources on the load servers side
				for(final V nextLoadBuilder : loadSvcMap.values()) {
					nextLoadBuilder.setInput(null);
				}
			}
		} catch(final NoSuchElementException e) {
			LOG.warn(Markers.ERR, "No \"data.src.fpath\" value was set");
		} catch(final IOException e) {
			LOG.warn(Markers.ERR, "Invalid items input file path: {}", itemInput);
		} catch(final SecurityException | NoSuchMethodException e) {
			LOG.warn(Markers.ERR, "Unexpected exception", e);
		}
		//
		final String dstFilePathStr = appConfig.getItemDstFile();
		if(dstFilePathStr != null && !dstFilePathStr.isEmpty()) {
			final Path dstFilePath = Paths.get(dstFilePathStr);
			try {
				if(Files.exists(dstFilePath) && Files.size(dstFilePath) > 0) {
					LOG.warn(
						Markers.ERR, "Items destination file \"{}\" is not empty", dstFilePathStr
					);
				}
				setOutput(
					new ItemCsvFileOutput<>(
						dstFilePath, (Class<T>) ioConfig.getItemClass(), ioConfig.getContentSource()
					)
				);
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to use CSV file output");
			}
		}
		//
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setLoadType(final LoadType loadType)
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
	public final LoadBuilderClient<T, W, U> setCountLimit(final long countLimit)
	throws IllegalArgumentException, RemoteException {
		super.setCountLimit(countLimit);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setCountLimit(countLimit);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setSizeLimit(final long sizeLimit)
	throws IllegalArgumentException, RemoteException {
		super.setSizeLimit(sizeLimit);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setSizeLimit(sizeLimit);
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
	public final LoadBuilderClient<T, W, U> setThreadCount(final int threadCount)
	throws IllegalArgumentException, RemoteException {
		super.setThreadCount(threadCount);
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setThreadCount(threadCount);
			}
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderClient<T, W, U> setNodeAddrs(final String[] nodeAddrs)
	throws IllegalArgumentException, RemoteException {
		super.setNodeAddrs(nodeAddrs);
		if(nodeAddrs != null && nodeAddrs.length > 0) {
			this.storageNodeAddrs = nodeAddrs;
			if(flagAssignLoadSvcToNode) {
				assignNodesToLoadSvcs(loadSvcConfMap, loadSvcAddrs, storageNodeAddrs);
			}
			//
			V nextBuilder;
			if(loadSvcMap != null) {
				for(final String addr : loadSvcMap.keySet()) {
					nextBuilder = loadSvcMap.get(addr);
					nextBuilder.setNodeAddrs(
						loadSvcConfMap.get(addr).getStorageAddrs()
					);
				}
			}
		}
		return this;
	}
	//
	@Override
	public String toString() {
		final StringBuilder strBuilder = new StringBuilder(ioConfig.toString());
		try {
			strBuilder.append('.').append(loadSvcMap.get(loadSvcMap.keySet().iterator().next())
				.getNextInstanceNum(appConfig.getRunId()));
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
