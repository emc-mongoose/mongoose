package com.emc.mongoose.object.load;
//
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.api.WSRequestConfigBase;
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.object.load.type.ws.Append;
import com.emc.mongoose.object.load.type.ws.Create;
import com.emc.mongoose.object.load.type.ws.Delete;
import com.emc.mongoose.object.load.type.ws.Read;
import com.emc.mongoose.object.load.type.ws.Update;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.commons.configuration.ConversionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
/**
 Created by kurila on 05.05.14.
 */
public class WSLoadBuilderImpl<T extends WSObjectImpl, U extends WSLoadExecutor<T>>
implements WSLoadBuilder<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected WSRequestConfig<T> reqConf;
	protected Request.Type loadType;
	protected long maxCount, minObjSize, maxObjSize;
	protected int updatesPerItem;
	protected String listFile, dataNodeAddrs[];
	protected final HashMap<Request.Type, Short> threadsPerNodeMap;
	//
	{
		threadsPerNodeMap = new HashMap<>();
		setProperties(new RunTimeConfig());
	}
	//
	@Override @SuppressWarnings("unchecked")
	public ObjectLoadBuilder<T, U> setProperties(final RunTimeConfig props) {
		reqConf = WSRequestConfigBase.getInstance();
		reqConf.setProperties(props);
		String paramName;
		for(final Request.Type loadType: Request.Type.values()) {
			paramName = "load."+loadType.name().toLowerCase()+".threads";
			try {
				setThreadsPerNodeFor(RunTimeConfig.getShort(paramName), loadType);
			} catch(final NoSuchElementException e) {
				LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
			} catch(final IllegalArgumentException e) {
				LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
			}
		}
		//
		paramName = "data.count";
		try {
			setMaxCount(RunTimeConfig.getLong(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "data.size.min";
		try {
			setMinObjSize(RunTimeConfig.getSizeBytes(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "data.size.max";
		try {
			setMaxObjSize(RunTimeConfig.getSizeBytes(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "load.update.per.item";
		try {
			setUpdatesPerItem(RunTimeConfig.getInt(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "storage.addrs";
		try {
			setDataNodeAddrs(RunTimeConfig.getStringArray(paramName));
		} catch(final NoSuchElementException|ConversionException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "api."+ reqConf.getAPI().toLowerCase()+".port";
		try {
			reqConf.setPort(RunTimeConfig.getInt(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		}
		//
		paramName = "storage.scheme";
		try {
			reqConf.setScheme(RunTimeConfig.getString(paramName));
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		paramName = "data.src.fpath";
		try {
			setInputFile(RunTimeConfig.getString(paramName));
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
	public WSLoadBuilderImpl<T, U> setRequestConfig(final RequestConfig<T> reqConf)
	throws ClassCastException {
		LOG.debug(Markers.MSG, "Set request builder: {}", reqConf.toString());
		this.reqConf = (WSRequestConfig<T>) reqConf;
		return this;
	}
	//
	@Override
	public WSLoadBuilderImpl<T, U> setLoadType(final Request.Type loadType)
	throws IllegalStateException {
		LOG.debug(Markers.MSG, "Set load type: {}", loadType);
		if(reqConf==null) {
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
	public WSLoadBuilderImpl<T, U> setMaxCount(final long maxCount)
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
	public WSLoadBuilderImpl<T, U> setMinObjSize(final long minObjSize)
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
	public WSLoadBuilderImpl<T, U> setMaxObjSize(final long maxObjSize)
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
	public WSLoadBuilderImpl<T, U> setThreadsPerNodeDefault(
		final short threadsPerNode
	) throws IllegalArgumentException {
		if(threadsPerNode<1) {
			throw new IllegalArgumentException("Thread count should not be less than 1");
		}
		LOG.debug(Markers.MSG, "Set default thread count per node: {}", threadsPerNode);
		for(final Request.Type loadType: Request.Type.values()) {
			threadsPerNodeMap.put(loadType, threadsPerNode);
		}
		return this;
	}
	//
	@Override
	public WSLoadBuilderImpl<T, U> setThreadsPerNodeFor(
		final short threadsPerNode, final Request.Type loadType
	) throws IllegalArgumentException {
		if(threadsPerNode<1) {
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
	public WSLoadBuilderImpl<T, U> setDataNodeAddrs(
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
	public WSLoadBuilderImpl<T, U> setInputFile(final String listFile) {
		LOG.debug(Markers.MSG, "Set consuming data items from file: {}", listFile);
		this.listFile = listFile;
		return this;
	}
	//
	@Override
	public WSLoadBuilderImpl<T, U> setUpdatesPerItem(final int count)
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
	public final WSLoadBuilderImpl<T, U> clone() {
		final WSLoadBuilderImpl<T, U> lb = new WSLoadBuilderImpl<>();
		LOG.debug(Markers.MSG, "Cloning request config for {}", reqConf.toString());
		lb.reqConf = reqConf.clone();
		lb.loadType = loadType;
		lb.maxCount = maxCount;
		lb.minObjSize = minObjSize;
		lb.maxObjSize = maxObjSize;
		for(final Request.Type loadType: threadsPerNodeMap.keySet()) {
			lb.threadsPerNodeMap.put(loadType, threadsPerNodeMap.get(loadType));
		}
		lb.dataNodeAddrs = dataNodeAddrs;
		lb.listFile = listFile;
		return lb;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public U build()
	throws IllegalStateException {
		if(reqConf==null) {
			throw new IllegalStateException("Should specify request builder instance");
		}
		//
		WSLoadExecutorBase<T> load = null;
		if(minObjSize<=maxObjSize) {
			try {
				switch(loadType) {
					case CREATE:
						LOG.debug("New create load");
						load = new Create<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, minObjSize, maxObjSize
						);
						break;
					case READ:
						LOG.debug("New read load");
						load = new Read<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case UPDATE:
						LOG.debug("New update load");
						load = new Update<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, updatesPerItem
						);
						break;
					case DELETE:
						LOG.debug("New delete load");
						load = new Delete<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case APPEND:
						LOG.debug("New append load");
						load = new Append<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, minObjSize, maxObjSize
						);
				}
			} catch(CloneNotSupportedException|IOException e) {
				throw new IllegalStateException(e);
			}
		} else {
			throw new IllegalStateException(
				"Min object size ("+Long.toString(minObjSize)+
				") should be less than upper bound "+Long.toString(maxObjSize)
			);
		}
		//
		return (U) load;
	}
}
