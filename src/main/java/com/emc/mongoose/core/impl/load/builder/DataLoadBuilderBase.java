package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.DataItemFileSrc;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.item.base.ItemCSVFileSrc;
import com.emc.mongoose.core.impl.item.data.NewDataItemSrc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
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
	protected int updatesPerItem;
	protected float objSizeBias;
	//
	public DataLoadBuilderBase(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
	}
	//
	@Override
	public DataLoadBuilderBase<T, U> clone()
	throws CloneNotSupportedException {
		final DataLoadBuilderBase<T, U> lb = (DataLoadBuilderBase<T, U>) super.clone();
		lb.minObjSize = minObjSize;
		lb.maxObjSize = maxObjSize;
		lb.updatesPerItem = updatesPerItem;
		lb.objSizeBias = objSizeBias;
		return lb;
	}
	//
	@SuppressWarnings("unchecked")
	private ItemSrc<T> getNewItemSrc()
	throws NoSuchMethodException {
		return new NewDataItemSrc<>(
			(Class<T>) ioConfig.getItemClass(), appConfig.getItemNaming(),
			ioConfig.getContentSource(), minObjSize, maxObjSize, objSizeBias
		);
	}
	//
	@SuppressWarnings("unchecked")
	private ItemSrc<T> getContainerItemSrc()
	throws CloneNotSupportedException {
		return (ItemSrc<T>) ((IOConfig) ioConfig.clone()).getContainerListInput(
			maxCount, storageNodeAddrs == null ? null : storageNodeAddrs[0]
		);
	}
	//
	protected ItemSrc<T> getDefaultItemSource() {
		try {
			if(flagUseNoneItemSrc) {
				return null;
			} else if(flagUseContainerItemSrc && flagUseNewItemSrc) {
				if(IOTask.Type.CREATE.equals(ioConfig.getLoadType())) {
					return getNewItemSrc();
				} else {
					return getContainerItemSrc();
				}
			} else if(flagUseNewItemSrc) {
				return getNewItemSrc();
			} else if(flagUseContainerItemSrc) {
				return getContainerItemSrc();
			}
		} catch(final NoSuchMethodException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to build the new data items source");
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the I/O config instance");
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
	public DataLoadBuilder<T, U> setAppConfig(final AppConfig appConfig)
	throws IllegalStateException, RemoteException {
		super.setAppConfig(appConfig);
		//
		final AppConfig.DataSizeScheme dataSizeScheme = appConfig.getItemDataSizeClass();
		final long minSize, maxSize;
		final double sizeBias;
		if(AppConfig.DataSizeScheme.FIXED.equals(dataSizeScheme)) {
			minSize = appConfig.getItemDataSizeFixed();
			maxSize = minSize;
			sizeBias = 0;
		} else {
			minSize = appConfig.getItemDataSizeRandomMin();
			maxSize = appConfig.getItemDataSizeRandomMax();
			sizeBias = appConfig.getItemDataSizeRandomBias();
		}
		//
		setMinObjSize(minSize);
		setMaxObjSize(maxSize);
		setObjSizeBias((float) sizeBias);
		//
		final AppConfig.DataRangesScheme dataRangesScheme = appConfig.getItemDataRangesClass();
		if(AppConfig.DataRangesScheme.FIXED.equals(dataRangesScheme)) {
			final String fixedRanges = appConfig.getItemDataRangesFixedBytes();
			// TODO implement
		} else {
			final int randomCount = appConfig.getItemDataContentRangesRandomCount();
			setUpdatesPerItem(randomCount);
		}
		//
		final String listFilePathStr = appConfig.getItemInputFile();
		if(itemsFileExists(listFilePathStr)) {
			try {
				setItemSrc(
					new ItemCSVFileSrc<>(
						Paths.get(listFilePathStr), (Class<T>) ioConfig.getItemClass(),
						ioConfig.getContentSource()
					)
				);
			} catch(final IOException | NoSuchMethodException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to use CSV file input");
			}
		}
		//
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setItemSrc(final ItemSrc<T> itemSrc)
	throws RemoteException {
		super.setItemSrc(itemSrc);
		if(itemSrc instanceof DataItemFileSrc) {
			final DataItemFileSrc<T> fileInput = (DataItemFileSrc<T>) itemSrc;
			final long approxDataItemsSize = fileInput.getApproxDataItemsSize(
				BasicConfig.THREAD_CONTEXT.get().getItemInputBatchSize()
			);
			ioConfig.setBuffSize(
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
}
