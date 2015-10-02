package com.emc.mongoose.core.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.concurrent.ThreadUtil;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.model.DataItemSrc;
import com.emc.mongoose.core.api.data.model.FileDataItemSrc;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.data.model.CSVFileItemSrc;
import com.emc.mongoose.core.impl.data.model.NewDataItemSrc;
import org.apache.commons.configuration.ConversionException;
//
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
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 20.10.14.
 */
public abstract class LoadBuilderBase<T extends DataItem, U extends LoadExecutor<T>>
implements LoadBuilder<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected RequestConfig<T> reqConf;
	protected long maxCount, minObjSize, maxObjSize;
	protected float objSizeBias, rateLimit;
	protected int manualTaskSleepMicroSecs, updatesPerItem;
	protected DataItemSrc itemSrc;
	protected String storageNodeAddrs[];
	protected final HashMap<IOTask.Type, Integer> loadTypeWorkerCount, loadTypeConnPerNode;
	protected boolean flagUseContainerItemSrc, flagUseNewItemSrc, flagUseNoneItemSrc;
	//
	{
		loadTypeWorkerCount = new HashMap<>();
		loadTypeConnPerNode = new HashMap<>();
		try {
			reqConf = getDefaultRequestConfig();
			setProperties(RunTimeConfig.getContext());
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to apply configuration");
		}
	}
	protected abstract RequestConfig<T> getDefaultRequestConfig();
	//
	public LoadBuilderBase(final RunTimeConfig runTimeConfig) {
		resetItemSrcFlags();
		setProperties(runTimeConfig);
	}
	//
	protected final void resetItemSrcFlags() {
		flagUseContainerItemSrc = true;
		flagUseNewItemSrc = true;
		flagUseNoneItemSrc = false;
	}
	//
	@Override
	public LoadBuilder<T, U> setProperties(final RunTimeConfig rtConfig)
	throws IllegalStateException {
		RunTimeConfig.setContext(rtConfig);
		if(reqConf != null) {
			reqConf.setProperties(rtConfig);
		} else {
			throw new IllegalStateException("Shared request config is not initialized");
		}
		//
		String paramName;
		for(final IOTask.Type loadType: IOTask.Type.values()) {
			paramName = RunTimeConfig.getConnCountPerNodeParamName(loadType.name().toLowerCase());
			try {
				setConnPerNodeFor(
					rtConfig.getConnCountPerNodeFor(loadType.name().toLowerCase()), loadType
				);
			} catch(final NoSuchElementException e) {
				LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
			} catch(final IllegalArgumentException e) {
				LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
			}
		}
		//
		for(final IOTask.Type loadType: IOTask.Type.values()) {
			paramName = RunTimeConfig.getLoadWorkersParamName(loadType.name().toLowerCase());
			try {
				setWorkerCountFor(
					rtConfig.getWorkerCountFor(loadType.name().toLowerCase()), loadType
				);
			} catch(final NoSuchElementException e) {
				LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
			} catch(final IllegalArgumentException e) {
				LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
			}
		}
		//
		paramName = RunTimeConfig.KEY_DATA_ITEM_COUNT;
		try {
			setMaxCount(rtConfig.getLoadLimitCount());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_MIN;
		try {
			setMinObjSize(rtConfig.getDataSizeMin());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_MAX;
		try {
			setMaxObjSize(rtConfig.getDataSizeMax());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_BIAS;
		try {
			setObjSizeBias(rtConfig.getDataSizeBias());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_LOAD_LIMIT_REQSLEEP_MILLISEC;
		try {
			setManualTaskSleepMicroSecs(
				(int) TimeUnit.MILLISECONDS.toMicros(
					rtConfig.getLoadLimitReqSleepMilliSec()
				)
			);
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_LOAD_LIMIT_RATE;
		try {
			setRateLimit(rtConfig.getLoadLimitRate());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_LOAD_UPDATE_PER_ITEM;
		try {
			setUpdatesPerItem(rtConfig.getInt(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_STORAGE_ADDRS;
		try {
			setDataNodeAddrs(rtConfig.getStorageAddrsWithPorts());
		} catch(final NoSuchElementException|ConversionException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.getApiPortParamName(reqConf.getAPI().toLowerCase());
		try {
			reqConf.setPort(rtConfig.getApiTypePort(reqConf.getAPI().toLowerCase()));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		final String listFilePathStr = rtConfig.getDataSrcFPath();
		if(listFilePathStr != null && !listFilePathStr.isEmpty()) {
			final Path listFilePath = Paths.get(listFilePathStr);
			if(!Files.exists(listFilePath)) {
				LOG.warn(Markers.ERR, "Specified input file \"{}\" doesn't exists", listFilePath);
			} else if(!Files.isReadable(listFilePath)) {
				LOG.warn(Markers.ERR, "Specified input file \"{}\" isn't readable", listFilePath);
			} else if(Files.isDirectory(listFilePath)) {
				LOG.warn(Markers.ERR, "Specified input file \"{}\" is a directory", listFilePath);
			} else {
				try {
					setItemSrc(new CSVFileItemSrc<>(listFilePath, reqConf.getDataItemClass()));
				} catch(final IOException | NoSuchMethodException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to use CSV file input");
				}
			}
		}
		//
		return this;
	}
	//
	@Override
	public RequestConfig<T> getRequestConfig() {
		return reqConf;
	}
	//
	@Override
	public LoadBuilder<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws ClassCastException {
		if(this.reqConf.equals(reqConf)) {
			return this;
		}
		LOG.debug(Markers.MSG, "Set request builder: {}", reqConf.toString());
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
	public LoadBuilder<T, U> setLoadType(final IOTask.Type loadType)
	throws IllegalStateException {
		LOG.debug(Markers.MSG, "Set load type: {}", loadType);
		if(reqConf == null) {
			throw new IllegalStateException(
				"Request builder should be specified before setting an I/O loadType"
			);
		} else {
			reqConf.setLoadType(loadType);
		}
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setMaxCount(final long maxCount)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set max data item count: {}", maxCount);
		if(maxCount < 0) {
			throw new IllegalArgumentException("Count should be >= 0");
		}
		this.maxCount = maxCount;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set min data item size: {}", SizeUtil.formatSize(minObjSize));
		if(minObjSize >= 0) {
			LOG.debug(Markers.MSG, "Using min object size: {}", SizeUtil.formatSize(minObjSize));
		} else {
			throw new IllegalArgumentException("Min object size should not be less than min");
		}
		this.minObjSize = minObjSize;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set max data item size: {}", SizeUtil.formatSize(maxObjSize));
		if(maxObjSize >= 0) {
			LOG.debug(Markers.MSG, "Using max object size: {}", SizeUtil.formatSize(maxObjSize));
		} else {
			throw new IllegalArgumentException("Max object size should not be less than min");
		}
		this.maxObjSize = maxObjSize;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setObjSizeBias(final float objSizeBias)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set object size bias: {}", objSizeBias);
		if(objSizeBias < 0) {
			throw new IllegalArgumentException("Object size bias should not be negative");
		} else {
			LOG.debug(Markers.MSG, "Using object size bias: {}", objSizeBias);
		}
		this.objSizeBias = objSizeBias;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setRateLimit(final float rateLimit)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set rate limit to: {}", rateLimit);
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Rate limit should not be negative");
		} else {
			LOG.debug(Markers.MSG, "Using load rate limit: {}", rateLimit);
		}
		this.rateLimit = rateLimit;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setManualTaskSleepMicroSecs(final int manualTaskSleepMicroSecs)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set manual I/O tasks sleep to: {}[us]", manualTaskSleepMicroSecs);
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Tasks sleep time shouldn't be negative");
		} else {
			LOG.debug(Markers.MSG, "Using tasks sleep time: {}[us]", manualTaskSleepMicroSecs);
		}
		this.manualTaskSleepMicroSecs = manualTaskSleepMicroSecs;
		return this;
	}
	//

	//
	@Override
	public LoadBuilder<T, U> setWorkerCountDefault(final int workersPerNode) {
		for(final IOTask.Type loadType: IOTask.Type.values()) {
			setWorkerCountFor(workersPerNode, loadType);
		}
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setWorkerCountFor(
		final int workersPerNode, final IOTask.Type loadType
	) {
		if(workersPerNode > 0) {
			loadTypeWorkerCount.put(loadType, workersPerNode);
		} else {
			loadTypeWorkerCount.put(loadType, ThreadUtil.getWorkerCount());
		}
		LOG.debug(
			Markers.MSG, "Set worker count per node {} for load type \"{}\"",
			workersPerNode, loadType
		);
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setConnPerNodeDefault(final int connPerNode)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set default connection count per node: {}", connPerNode);
		for(final IOTask.Type loadType : IOTask.Type.values()) {
			setConnPerNodeFor(connPerNode, loadType);
		}
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setConnPerNodeFor(
		final int connPerNode, final IOTask.Type loadType
	) throws IllegalArgumentException {
		if(connPerNode < 1) {
			throw new IllegalArgumentException("Concurrency level should not be less than 1");
		}
		LOG.debug(
			Markers.MSG, "Set connection count per node {} for load type \"{}\"",
			connPerNode, loadType
		);
		loadTypeConnPerNode.put(loadType, connPerNode);
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setDataNodeAddrs(
		final String[] dataNodeAddrs
	) throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set storage nodes: {}", Arrays.toString(dataNodeAddrs));
		if(dataNodeAddrs == null || dataNodeAddrs.length == 0) {
			throw new IllegalArgumentException("Data node address list should not be empty");
		}
		this.storageNodeAddrs = dataNodeAddrs;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setItemSrc(final DataItemSrc<T> itemSrc) {
		LOG.debug(Markers.MSG, "Set data items source: {}", itemSrc);
		this.itemSrc = itemSrc;
		if(itemSrc instanceof FileDataItemSrc) {
			final FileDataItemSrc<T> fileInput = (FileDataItemSrc<T>) itemSrc;
			final long approxDataItemsSize = fileInput.getApproxDataItemsSize(
				RunTimeConfig.getContext().getBatchSize()
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
	@Override
	public LoadBuilder<T, U> setUpdatesPerItem(final int count)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set updates count per data item: {}", count);
		if(count<0) {
			throw new IllegalArgumentException("Update count per item should not be less than 0");
		}
		this.updatesPerItem = count;
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public LoadBuilderBase<T, U> clone()
	throws CloneNotSupportedException {
		final LoadBuilderBase<T, U> lb = (LoadBuilderBase<T, U>) super.clone();
		LOG.debug(Markers.MSG, "Cloning request config for {}", reqConf.toString());
		lb.reqConf = reqConf.clone();
		lb.maxCount = maxCount;
		lb.minObjSize = minObjSize;
		lb.maxObjSize = maxObjSize;
		for(final IOTask.Type loadType : loadTypeWorkerCount.keySet()) {
			lb.loadTypeWorkerCount.put(loadType, loadTypeWorkerCount.get(loadType));
		}
		for(final IOTask.Type loadType : loadTypeConnPerNode.keySet()) {
			lb.loadTypeConnPerNode.put(loadType, loadTypeConnPerNode.get(loadType));
		}
		lb.storageNodeAddrs = storageNodeAddrs;
		lb.itemSrc = itemSrc;
		lb.rateLimit = rateLimit;
		lb.manualTaskSleepMicroSecs = manualTaskSleepMicroSecs;
		return lb;
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
	public LoadBuilderBase<T, U> useNewItemSrc()
	throws RemoteException {
		flagUseNewItemSrc = true;
		return this;
	}
	//
	@Override
	public LoadBuilderBase<T, U> useNoneItemSrc()
	throws RemoteException {
		flagUseNoneItemSrc = true;
		return this;
	}
	//
	@Override
	public LoadBuilderBase<T, U> useContainerListingItemSrc()
	throws RemoteException {
		flagUseContainerItemSrc = true;
		return this;
	}
	//
	@Override
	public final U build() {
		try {
			invokePreConditions();
		} catch(final IllegalStateException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Preconditions failure");
		}
		try {
			return buildActually();
		} finally {
			resetItemSrcFlags();
		}
	}
	//
	protected abstract void invokePreConditions()
	throws IllegalStateException;
	//
	protected final int getMinIOThreadCount(
		final int threadCount, final int nodeCount, final int connCountPerNode
	) {
		return Math.min(Math.max(threadCount, nodeCount), nodeCount * connCountPerNode);
	}
	//
	protected abstract U buildActually();
	//
	private final static String FMT_STR = "%s.%dx%s", FMT_SIZE_RANGE = "%s-%s";
	//
	@Override
	public String toString() {
		return String.format(
			FMT_STR,
			reqConf.toString(),
			loadTypeConnPerNode.get(loadTypeConnPerNode.keySet().iterator().next()),
			minObjSize == maxObjSize ?
				SizeUtil.formatSize(minObjSize) :
				String.format(FMT_SIZE_RANGE, minObjSize, maxObjSize)
		);
	}
	//
	@Override
	public void close()
	throws IOException {
		reqConf.close();
	}
}
