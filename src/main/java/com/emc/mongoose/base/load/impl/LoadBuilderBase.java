package com.emc.mongoose.base.load.impl;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.api.AsyncIOTask;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.data.persist.TmpFileItemBuffer;
import com.emc.mongoose.base.load.DataItemBuffer;
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.commons.configuration.ConversionException;
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
	protected AsyncIOTask.Type loadType;
	protected long maxCount, minObjSize, maxObjSize;
	protected float objSizeBias;
	protected int updatesPerItem;
	protected String listFile, dataNodeAddrs[];
	protected final HashMap<AsyncIOTask.Type, Short> threadsPerNodeMap;
	//
	{
		threadsPerNodeMap = new HashMap<>();
		try {
			reqConf = getDefaultRequestConfig();
			setProperties(Main.RUN_TIME_CONFIG.get());
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to apply configuration");
		}
	}
	protected abstract RequestConfig<T> getDefaultRequestConfig();
	//
	@Override
	public LoadBuilder<T, U> setProperties(final RunTimeConfig runTimeConfig)
	throws IllegalStateException {
		Main.RUN_TIME_CONFIG.set(runTimeConfig);
		if(reqConf != null) {
			reqConf.setProperties(runTimeConfig);
		} else {
			throw new IllegalStateException("Shared request config is not initialized");
		}
		//
		String paramName;
		for(final AsyncIOTask.Type loadType: AsyncIOTask.Type.values()) {
			paramName = "load."+loadType.name().toLowerCase()+".threads";
			try {
				setThreadsPerNodeFor(runTimeConfig.getShort(paramName), loadType);
			} catch(final NoSuchElementException e) {
				LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
			} catch(final IllegalArgumentException e) {
				LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
			}
		}
		//
		paramName = RunTimeConfig.KEY_DATA_COUNT;
		try {
			setMaxCount(runTimeConfig.getDataCount());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "data.size.min";
		try {
			setMinObjSize(runTimeConfig.getSizeBytes(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "data.size.max";
		try {
			setMaxObjSize(runTimeConfig.getSizeBytes(paramName));
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
		paramName = "load.update.per.item";
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
			setDataNodeAddrs(runTimeConfig.getStorageAddrs());
		} catch(final NoSuchElementException|ConversionException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "api."+ reqConf.getAPI().toLowerCase()+".port";
		try {
			reqConf.setPort(runTimeConfig.getApiPort(reqConf.getAPI().toLowerCase()));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		paramName = "data.src.fpath";
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
		LOG.debug(Markers.MSG, "Set request builder: {}", reqConf.toString());
		this.reqConf = reqConf;
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setLoadType(final AsyncIOTask.Type loadType)
		throws IllegalStateException {
		LOG.debug(Markers.MSG, "Set load type: {}", loadType);
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
		LOG.debug(Markers.MSG, "Set min data item size: {}", RunTimeConfig.formatSize(minObjSize));
		if(minObjSize > 0) {
			LOG.debug(Markers.MSG, "Using min object size: {}", RunTimeConfig.formatSize(minObjSize));
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
		LOG.debug(Markers.MSG, "Set max data item size: {}", RunTimeConfig.formatSize(maxObjSize));
		if(maxObjSize > 0) {
			LOG.debug(Markers.MSG, "Using max object size: {}", RunTimeConfig.formatSize(maxObjSize));
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
	public LoadBuilder<T, U> setThreadsPerNodeDefault(final short threadsPerNode)
	throws IllegalArgumentException {
		if(threadsPerNode < 1) {
			throw new IllegalArgumentException("Thread count should not be less than 1");
		}
		LOG.debug(Markers.MSG, "Set default thread count per node: {}", threadsPerNode);
		for(final AsyncIOTask.Type loadType: AsyncIOTask.Type.values()) {
			threadsPerNodeMap.put(loadType, threadsPerNode);
		}
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setThreadsPerNodeFor(
		final short threadsPerNode, final AsyncIOTask.Type loadType
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
		if(dataNodeAddrs==null || dataNodeAddrs.length==0) {
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
		lb.loadType = loadType;
		lb.maxCount = maxCount;
		lb.minObjSize = minObjSize;
		lb.maxObjSize = maxObjSize;
		for(final AsyncIOTask.Type loadType: threadsPerNodeMap.keySet()) {
			lb.threadsPerNodeMap.put(loadType, threadsPerNodeMap.get(loadType));
		}
		lb.dataNodeAddrs = dataNodeAddrs;
		lb.listFile = listFile;
		return lb;
	}
	//
	@Override
	public abstract U build()
	throws IllegalStateException;
	//
	@Override
	public DataItemBuffer<T> newDataItemBuffer()
	throws IOException {
		return new TmpFileItemBuffer<>(getMaxCount(), 1);
	}
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
				RunTimeConfig.formatSize(minObjSize) :
				String.format(FMT_SIZE_RANGE, minObjSize, maxObjSize)
		);
	}
}
