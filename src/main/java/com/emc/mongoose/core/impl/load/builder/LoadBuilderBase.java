package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.persist.DataItemBuffer;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.persist.TmpFileItemBuffer;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.commons.configuration.ConversionException;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
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
	protected IOTask.Type loadType;
	protected long maxCount, minObjSize, maxObjSize;
	protected float objSizeBias, rateLimit;
	protected int updatesPerItem;
	protected String listFile, dataNodeAddrs[];
	protected final HashMap<IOTask.Type, Short> threadsPerNodeMap;
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
					runTimeConfig.getLoadTypeThreads(
						loadType.name().toLowerCase()
					), loadType
				);
			} catch(final NoSuchElementException e) {
				LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
			} catch(final IllegalArgumentException e) {
				LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
			}
		}
		//
		paramName = RunTimeConfig.KEY_DATA_ITEM_COUNT;
		try {
			setMaxCount(runTimeConfig.getLoadLimitCount());
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_MIN;
		try {
			setMinObjSize(runTimeConfig.getDataSizeMin());
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_MAX;
		try {
			setMaxObjSize(runTimeConfig.getDataSizeMax());
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SIZE_BIAS;
		try {
			setObjSizeBias(runTimeConfig.getDataSizeBias());
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_LOAD_LIMIT_RATE;
		try {
			setRateLimit(runTimeConfig.getLoadLimitRate());
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_LOAD_UPDATE_PER_ITEM;
		try {
			setUpdatesPerItem(runTimeConfig.getInt(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.KEY_STORAGE_ADDRS;
		try {
			setDataNodeAddrs(runTimeConfig.getStorageAddrs());
		} catch(final NoSuchElementException|ConversionException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = RunTimeConfig.getApiPortParamName(reqConf.getAPI().toLowerCase());
		try {
			reqConf.setPort(runTimeConfig.getApiTypePort(reqConf.getAPI().toLowerCase()));
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		paramName = RunTimeConfig.KEY_DATA_SRC_FPATH;
		try {
			setInputFile(runTimeConfig.getString(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(LogUtil.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
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
		LOG.debug(LogUtil.MSG, "Set request builder: {}", reqConf.toString());
		this.reqConf = reqConf;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setLoadType(final IOTask.Type loadType)
		throws IllegalStateException {
		LOG.debug(LogUtil.MSG, "Set load type: {}", loadType);
		if(reqConf == null) {
			throw new IllegalStateException(
				"Request builder should be specified before setting an I/O loadType"
			);
		} else {
			reqConf.setLoadType(loadType);
			this.loadType = loadType;
		}
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setMaxCount(final long maxCount)
		throws IllegalArgumentException {
		LOG.debug(LogUtil.MSG, "Set max data item count: {}", maxCount);
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
		LOG.debug(LogUtil.MSG, "Set min data item size: {}", SizeUtil.formatSize(minObjSize));
		if(minObjSize >= 0) {
			LOG.debug(LogUtil.MSG, "Using min object size: {}", SizeUtil.formatSize(minObjSize));
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
		LOG.debug(LogUtil.MSG, "Set max data item size: {}", SizeUtil.formatSize(maxObjSize));
		if(maxObjSize >= 0) {
			LOG.debug(LogUtil.MSG, "Using max object size: {}", SizeUtil.formatSize(maxObjSize));
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
		LOG.debug(LogUtil.MSG, "Set object size bias: {}", objSizeBias);
		if(objSizeBias < 0) {
			throw new IllegalArgumentException("Object size bias should not be negative");
		} else {
			LOG.debug(LogUtil.MSG, "Using object size bias: {}", objSizeBias);
		}
		this.objSizeBias = objSizeBias;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setRateLimit(final float rateLimit)
	throws IllegalArgumentException {
		LOG.debug(LogUtil.MSG, "Set rate limit to: {}", rateLimit);
		if(rateLimit < 0) {
			throw new IllegalArgumentException("Rate limit should not be negative");
		} else {
			LOG.debug(LogUtil.MSG, "Using load rate limit: {}", rateLimit);
		}
		this.rateLimit = rateLimit;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setThreadsPerNodeDefault(final short threadsPerNode)
	throws IllegalArgumentException {
		if(threadsPerNode < 1) {
			throw new IllegalArgumentException("Thread count should not be less than 1");
		}
		LOG.debug(LogUtil.MSG, "Set default thread count per node: {}", threadsPerNode);
		for(final IOTask.Type loadType: IOTask.Type.values()) {
			threadsPerNodeMap.put(loadType, threadsPerNode);
		}
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setThreadsPerNodeFor(
		final short threadsPerNode, final IOTask.Type loadType
	) throws IllegalArgumentException {
		if(threadsPerNode < 1) {
			throw new IllegalArgumentException("Thread count should not be less than 1");
		}
		LOG.debug(
			LogUtil.MSG, "Set thread count per node {} for load type \"{}\"",
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
		LOG.debug(LogUtil.MSG, "Set storage nodes: {}", Arrays.toString(dataNodeAddrs));
		if(dataNodeAddrs == null || dataNodeAddrs.length == 0) {
			throw new IllegalArgumentException("Data node address list should not be empty");
		}
		this.dataNodeAddrs = dataNodeAddrs;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setInputFile(final String listFile) {
		LOG.debug(LogUtil.MSG, "Set consuming data items from file: {}", listFile);
		this.listFile = listFile;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setUpdatesPerItem(final int count)
		throws IllegalArgumentException {
		LOG.debug(LogUtil.MSG, "Set updates count per data item: {}", count);
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
		LOG.debug(LogUtil.MSG, "Cloning request config for {}", reqConf.toString());
		lb.reqConf = reqConf.clone();
		lb.loadType = loadType;
		lb.maxCount = maxCount;
		lb.minObjSize = minObjSize;
		lb.maxObjSize = maxObjSize;
		for(final IOTask.Type loadType: threadsPerNodeMap.keySet()) {
			lb.threadsPerNodeMap.put(loadType, threadsPerNodeMap.get(loadType));
		}
		lb.dataNodeAddrs = dataNodeAddrs;
		lb.listFile = listFile;
		return lb;
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
	@Override
	public DataItemBuffer<T> newDataItemBuffer()
	throws IOException {
		return new TmpFileItemBuffer<>(maxCount);
	}
	//
	private final static int MAX_LOAD_COUNT = 10;
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
