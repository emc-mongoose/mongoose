package com.emc.mongoose.core.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.api.io.req.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.model.CSVFileItemInput;
import com.emc.mongoose.core.impl.data.model.NewDataItemInput;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
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
	protected int updatesPerItem;
	protected String listFile, dataNodeAddrs[];
	protected final HashMap<IOTask.Type, Integer> threadsPerNodeMap;
	//
	{
		threadsPerNodeMap = new HashMap<>();
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
		setProperties(runTimeConfig);
	}
	//
	@Override
	public LoadBuilder<T, U> setProperties(final RunTimeConfig runTimeConfig)
	throws IllegalStateException {
		RunTimeConfig.setContext(runTimeConfig);
		if(reqConf != null) {
			reqConf.setProperties(runTimeConfig);
		} else {
			throw new IllegalStateException("Shared request config is not initialized");
		}
		//
		String paramName;
		for(final IOTask.Type loadType: IOTask.Type.values()) {
			paramName = RunTimeConfig.getLoadThreadsParamName(loadType.name().toLowerCase());
			try {
				setThreadsPerNodeFor(
					runTimeConfig.getThreadCountFor(
						loadType.name().toLowerCase()
					), loadType
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
			setMaxCount(runTimeConfig.getLoadLimitCount());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_MIN;
		try {
			setMinObjSize(runTimeConfig.getDataSizeMin());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_MAX;
		try {
			setMaxObjSize(runTimeConfig.getDataSizeMax());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_BIAS;
		try {
			setObjSizeBias(runTimeConfig.getDataSizeBias());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_LOAD_LIMIT_RATE;
		try {
			setRateLimit(runTimeConfig.getLoadLimitRate());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_LOAD_UPDATE_PER_ITEM;
		try {
			setUpdatesPerItem(runTimeConfig.getInt(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_STORAGE_ADDRS;
		try {
			setDataNodeAddrs(runTimeConfig.getStorageAddrsWithPorts());
		} catch(final NoSuchElementException|ConversionException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.getApiPortParamName(reqConf.getAPI().toLowerCase());
		try {
			reqConf.setPort(runTimeConfig.getApiTypePort(reqConf.getAPI().toLowerCase()));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SRC_FPATH;
		try {
			setInputFile(runTimeConfig.getString(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
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
	public final long getMaxCount() {
		return maxCount;
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
	public LoadBuilder<T, U> setThreadsPerNodeDefault(final int threadsPerNode)
	throws IllegalArgumentException {
		if(threadsPerNode < 1) {
			throw new IllegalArgumentException("Thread count should not be less than 1");
		}
		LOG.debug(Markers.MSG, "Set default thread count per node: {}", threadsPerNode);
		for(final IOTask.Type loadType: IOTask.Type.values()) {
			threadsPerNodeMap.put(loadType, threadsPerNode);
		}
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setThreadsPerNodeFor(
		final int threadsPerNode, final IOTask.Type loadType
	) throws IllegalArgumentException {
		if(threadsPerNode < 1) {
			throw new IllegalArgumentException("Thread count should not be less than 1");
		}
		LOG.debug(
			Markers.MSG, "Set thread count per node {} for load type \"{}\"",
			threadsPerNode, loadType
		);
		threadsPerNodeMap.put(loadType, threadsPerNode);
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
		this.dataNodeAddrs = dataNodeAddrs;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setInputFile(final String listFile) {
		LOG.debug(Markers.MSG, "Set consuming data items from file: {}", listFile);
		this.listFile = listFile;
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
		for(final IOTask.Type loadType : threadsPerNodeMap.keySet()) {
			lb.threadsPerNodeMap.put(loadType, threadsPerNodeMap.get(loadType));
		}
		lb.dataNodeAddrs = dataNodeAddrs;
		lb.listFile = listFile;
		return lb;
	}
	//
	public static <T extends DataItem> DataItemInput<T> buildItemInput(
		final Class<T> dataCls, final RequestConfig<T> reqConf,
		final String nodeAddrs[], final String listFile, final long maxCount,
	    final long minObjSize, final long maxObjSize, final float objSizeBias
	) {
		DataItemInput<T> itemSrc = null;
		if(listFile != null && !listFile.isEmpty()) {
			final Path listFilePath = Paths.get(listFile);
			if(!Files.exists(listFilePath)) {
				LOG.warn(Markers.ERR, "Specified input file \"{}\" doesn't exists", listFilePath);
			} else if(!Files.isReadable(listFilePath)) {
				LOG.warn(Markers.ERR, "Specified input file \"{}\" isn't readable", listFilePath);
			} else {
				try {
					itemSrc = new CSVFileItemInput<>(Paths.get(listFile), dataCls);
				} catch(final IOException | NoSuchMethodException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to use CSV file input");
				}
			}
		} else if(IOTask.Type.CREATE.equals(reqConf.getLoadType())) {
			try {
				itemSrc = new NewDataItemInput<>(dataCls, minObjSize, maxObjSize, objSizeBias);
			} catch(final NoSuchMethodException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to use new data input");
			}
		} else if(reqConf.isContainerListingEnabled()) {
			itemSrc = reqConf.getContainerListInput(maxCount, nodeAddrs[0]);
		}
		return itemSrc;
	}
	//
	@Override
	public final U build() {
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
	protected abstract U buildActually();
	//
	private final static String FMT_STR = "%s.%dx%s", FMT_SIZE_RANGE = "%s-%s";
	//
	@Override
	public String toString() {
		return String.format(
			FMT_STR,
			reqConf.toString(),
			threadsPerNodeMap.get(threadsPerNodeMap.keySet().iterator().next()),
			minObjSize == maxObjSize ?
				SizeUtil.formatSize(minObjSize) :
				String.format(FMT_SIZE_RANGE, minObjSize, maxObjSize)
		);
	}
	//
	@Override
	public final void close()
	throws IOException {
		reqConf.close();
	}
}
