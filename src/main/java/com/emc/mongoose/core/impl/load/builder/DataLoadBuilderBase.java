package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.data.model.NewDataItemSrc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import java.util.NoSuchElementException;
/**
 Created by kurila on 20.10.15.
 */
public abstract class DataLoadBuilderBase<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilderBase<T, U>
implements DataLoadBuilder<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected long minObjSize, maxObjSize;
	protected float objSizeBias;
	protected boolean flagUseContainerItemSrc;
	//
	public DataLoadBuilderBase(final RunTimeConfig rtConfig) {
		super(rtConfig);
	}
	//
	@Override
	protected void resetItemSrc() {
		super.resetItemSrc();
		flagUseContainerItemSrc = true;
	}
	//
	@Override
	public DataLoadBuilderBase<T, U> clone()
	throws CloneNotSupportedException {
		final DataLoadBuilderBase<T, U> lb = (DataLoadBuilderBase<T, U>) super.clone();
		lb.minObjSize = minObjSize;
		lb.maxObjSize = maxObjSize;
		lb.objSizeBias = objSizeBias;
		return lb;
	}
	//
	protected ItemSrc<T> getDefaultItemSource() {
		try {
			if(flagUseNoneItemSrc) {
				return null;
			} else if(flagUseContainerItemSrc && flagUseNewItemSrc) {
				if(IOTask.Type.CREATE.equals(reqConf.getLoadType())) {
					return new NewDataItemSrc<>(
						reqConf.getItemClass(), reqConf.getContentSource(),
						minObjSize, maxObjSize, objSizeBias
					);
				} else {
					return reqConf.getContainerListInput(maxCount, storageNodeAddrs[0]);
				}
			} else if(flagUseNewItemSrc) {
				return new NewDataItemSrc<>(
					reqConf.getItemClass(), reqConf.getContentSource(),
					minObjSize, maxObjSize, objSizeBias
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
	public String toString() {
		return super.toString() + "x" +
			(minObjSize == maxObjSize ? minObjSize : minObjSize + "-" + maxObjSize);
	}
	//
	@Override
	public DataLoadBuilder<T, U> setProperties(final RunTimeConfig rtConfig)
	throws IllegalStateException {
		super.setProperties(rtConfig);
		//
		String paramName = RunTimeConfig.KEY_DATA_SIZE_MIN;
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
		paramName = RunTimeConfig.KEY_LOAD_UPDATE_PER_ITEM;
		try {
			setUpdatesPerItem(rtConfig.getInt(paramName));
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
	public DataLoadBuilder<T, U> setMinObjSize(final long minObjSize)
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
	public DataLoadBuilder<T, U> setMaxObjSize(final long maxObjSize)
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
	public DataLoadBuilder<T, U> setObjSizeBias(final float objSizeBias)
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
	public DataLoadBuilder<T, U> setUpdatesPerItem(final int count)
		throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set updates count per data item: {}", count);
		if(count<0) {
			throw new IllegalArgumentException("Update count per item should not be less than 0");
		}
		this.updatesPerItem = count;
		return this;
	}
	//
	@Override
	public DataLoadBuilder<T, U> useContainerListingItemSrc()
	throws RemoteException {
		flagUseContainerItemSrc = true;
		return this;
	}
}
