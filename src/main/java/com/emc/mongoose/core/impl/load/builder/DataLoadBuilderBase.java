package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.SizeInBytes;
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
	protected SizeInBytes dataSize;
	protected String rangesInfo;
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
		lb.dataSize = dataSize;
		lb.rangesInfo = rangesInfo;
		return lb;
	}
	//
	@SuppressWarnings("unchecked")
	private ItemSrc<T> getNewItemSrc()
	throws NoSuchMethodException {
		return new NewDataItemSrc<>(
			(Class<T>) ioConfig.getItemClass(), appConfig.getItemNaming(),
			ioConfig.getContentSource(), dataSize
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
				if(IOTask.Type.WRITE.equals(ioConfig.getLoadType())) {
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
		return super.toString() + "x" + dataSize.toString();
	}
	//
	@Override
	public DataLoadBuilder<T, U> setAppConfig(final AppConfig appConfig)
	throws IllegalStateException, RemoteException {
		super.setAppConfig(appConfig);
		setDataSize(new SizeInBytes(appConfig.getItemDataSize()));
		setDataRanges(appConfig.getItemDataRanges());
		//
		final String listFilePathStr = appConfig.getItemSrcFile();
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
			final long approxDataItemsSize = fileInput.getAvgDataSize(
				BasicConfig.THREAD_CONTEXT.get().getItemSrcBatchSize()
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
	public DataLoadBuilder<T, U> setDataSize(final SizeInBytes dataSize)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set data item size: {}", dataSize.toString());
		this.dataSize = dataSize;
		return this;
	}
	@Override
	public DataLoadBuilder<T, U> setDataRanges(final String dataRanges) {
		LOG.debug(Markers.MSG, "Set fixed byte ranges: {}", dataRanges);
		this.rangesInfo = dataRanges;
		return this;
	}
}
